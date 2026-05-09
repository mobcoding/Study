
package okhttp3;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.jvm.JvmStatic;
import kotlin.reflect.KClass;
import okhttp3.internal.Tags;
import okhttp3.internal.cache.DiskLruCache;

@SuppressWarnings("KotlinInternalInJava")
public class OkHttpUtils {

    /**
     * 标签集合 - 返回一个包装的可变Map，修改会同步到Builder
     */
    @JvmStatic
    public static Map<Class<?>, Object> tags(Request.Builder builder) {
        return new BuilderTagsMap(builder);
    }

    /**
     * 通过反射返回Request的标签可变集合
     * 注意：Request的tags是不可变的，这里返回的Map修改不会影响原Request
     */
    @JvmStatic
    public static Map<Class<?>, Object> tags(Request request) {
        return new RequestTagsMap(request);
    }

    /**
     * 包装Request.Builder的Tags，提供可变Map接口
     */
    private static class BuilderTagsMap extends LinkedHashMap<Class<?>, Object> {
        private final Request.Builder builder;

        BuilderTagsMap(Request.Builder builder) {
            this.builder = builder;
            // 初始化时从builder中提取现有tags
            extractExistingTags();
        }

        private void extractExistingTags() {
            Tags tags = builder.getTags$okhttp();
            extractTagsFromTagsObject(tags, this);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Object put(Class<?> key, Object value) {
            // 同步到builder - 使用raw type来避免泛型类型推断问题
            builder.tag((Class) key, value);
            return super.put(key, value);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Object remove(Object key) {
            if (key instanceof Class) {
                // 通过设置null来移除tag - 使用raw type来避免泛型类型推断问题
                builder.tag((Class) key, null);
            }
            return super.remove(key);
        }
    }

    /**
     * 包装Request的Tags，提供只读Map接口（因为Request是不可变的）
     */
    private static class RequestTagsMap extends LinkedHashMap<Class<?>, Object> {
        RequestTagsMap(Request request) {
            Tags tags = request.getTags$okhttp();
            extractTagsFromTagsObject(tags, this);
        }
    }

    /**
     * 从Tags对象中提取所有tag到Map中
     */
    @SuppressWarnings("unchecked")
    private static void extractTagsFromTagsObject(Tags tagsObject, Map<Class<?>, Object> targetMap) {
        try {
            // LinkedTags是一个链表结构，遍历所有节点
            Object current = tagsObject;
            while (current != null) {
                Class<?> currentClass = current.getClass();

                // 尝试获取type字段（标签的类型）
                try {
                    Field typeField = currentClass.getDeclaredField("type");
                    typeField.setAccessible(true);
                    KClass<?> type = (KClass<?>) typeField.get(current);

                    // 尝试获取tag字段（标签的值）
                    Field tagField = currentClass.getDeclaredField("tag");
                    tagField.setAccessible(true);
                    Object tag = tagField.get(current);

                    if (type != null) {
                        Class<?> javaClass = JvmClassMappingKt.getJavaClass(type);
                        targetMap.put(javaClass, tag);
                    }
                } catch (NoSuchFieldException e) {
                    // 如果没有type/tag字段，可能已经到达链表末尾或者是空的Tags对象
                    break;
                }

                // 尝试获取next字段（链表的下一个节点）
                try {
                    Field nextField = currentClass.getDeclaredField("next");
                    nextField.setAccessible(true);
                    current = nextField.get(current);
                } catch (NoSuchFieldException e) {
                    // 没有next字段，链表结束
                    break;
                }
            }
        } catch (IllegalAccessException e) {
            // 如果反射访问失败，返回空map
            e.printStackTrace();
        }
    }

    /**
     * 全部的请求头
     */
    @JvmStatic
    public static Headers.Builder headers(Request.Builder builder) {
        return builder.getHeaders$okhttp();
    }

    @JvmStatic
    public static Headers.Builder addLenient(Headers.Builder builder, String line) {
        return builder.addLenient$okhttp(line);
    }

    @JvmStatic
    public static DiskLruCache diskLruCache(Cache cache) {
        return cache.getCache$okhttp();
    }
}
