cmake_minimum_required(VERSION 3.4.1)

add_library(hello-jnicallback SHARED
            hello-jnicallback.c)
add_library(testLib SHARED
            testLib.c)

# Include libraries needed for hello-jnicallback lib
target_link_libraries(hello-jnicallback
                      testLib
                      android
                      log)
