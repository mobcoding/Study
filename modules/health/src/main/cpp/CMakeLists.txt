cmake_minimum_required(VERSION 3.18.1)
project("memory_stress")
set(PAGE_ALIGN_FLAGS "-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${PAGE_ALIGN_FLAGS}")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${PAGE_ALIGN_FLAGS}")

add_library(
        memory_stress
        SHARED
        memory_stress.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        memory_stress
        ${log-lib})