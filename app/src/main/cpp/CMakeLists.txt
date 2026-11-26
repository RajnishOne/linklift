cmake_minimum_required(VERSION 3.22.1)

project("linklift-mp3")

set(CMAKE_C_STANDARD 99)
set(CMAKE_CXX_STANDARD 17)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -DHAVE_CONFIG_H -fvisibility=hidden -Wno-attributes")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -fvisibility=hidden")

set(LAME_DIR ${CMAKE_CURRENT_SOURCE_DIR}/lame)

include_directories(
    ${LAME_DIR}
    ${LAME_DIR}/include
    ${LAME_DIR}/libmp3lame
)

set(LAME_SOURCES
    ${LAME_DIR}/libmp3lame/VbrTag.c
    ${LAME_DIR}/libmp3lame/bitstream.c
    ${LAME_DIR}/libmp3lame/encoder.c
    ${LAME_DIR}/libmp3lame/fft.c
    ${LAME_DIR}/libmp3lame/gain_analysis.c
    ${LAME_DIR}/libmp3lame/id3tag.c
    ${LAME_DIR}/libmp3lame/lame.c
    ${LAME_DIR}/libmp3lame/newmdct.c
    ${LAME_DIR}/libmp3lame/presets.c
    ${LAME_DIR}/libmp3lame/psymodel.c
    ${LAME_DIR}/libmp3lame/quantize.c
    ${LAME_DIR}/libmp3lame/quantize_pvt.c
    ${LAME_DIR}/libmp3lame/reservoir.c
    ${LAME_DIR}/libmp3lame/set_get.c
    ${LAME_DIR}/libmp3lame/tables.c
    ${LAME_DIR}/libmp3lame/takehiro.c
    ${LAME_DIR}/libmp3lame/util.c
    ${LAME_DIR}/libmp3lame/vbrquantize.c
    ${LAME_DIR}/libmp3lame/version.c
)

add_library(
    linklift-mp3
    SHARED
    mp3_encoder_jni.cpp
    ${LAME_SOURCES}
)

find_library(log-lib log)

target_link_libraries(
    linklift-mp3
    ${log-lib}
)

set(QJS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/quickjs)

set(QJS_SOURCES
    ${QJS_DIR}/dtoa.c
    ${QJS_DIR}/libregexp.c
    ${QJS_DIR}/libunicode.c
    ${QJS_DIR}/quickjs.c
    ${QJS_DIR}/quickjs-libc.c
    ${QJS_DIR}/gen/repl.c
    ${QJS_DIR}/gen/standalone.c
    ${QJS_DIR}/qjs.c
)

add_executable(
    qjs
    ${QJS_SOURCES}
)

set_target_properties(qjs PROPERTIES
    OUTPUT_NAME "qjs"
    PREFIX "lib"
    SUFFIX ".so"
    C_STANDARD 11
)

target_compile_definitions(qjs PRIVATE _GNU_SOURCE QUICKJS_NG_BUILD)
target_compile_options(qjs PRIVATE -funsigned-char -Wno-implicit-fallthrough -Wno-sign-compare)
target_include_directories(qjs PRIVATE ${QJS_DIR})
target_link_libraries(qjs PRIVATE m dl)

