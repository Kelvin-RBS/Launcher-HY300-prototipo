cmake_minimum_required(VERSION 3.18.1)
project("hy300emu")

add_library(hy300emu SHARED emu.cpp)
target_compile_options(hy300emu PRIVATE -O2)
find_library(log-lib log)
target_link_libraries(hy300emu android ${log-lib} dl)
