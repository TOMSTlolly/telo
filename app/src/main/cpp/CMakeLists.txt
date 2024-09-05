# example cmakelists:
# https://github.com/android/ndk-samples/blob/main/endless-tunnel/app/src/main/cpp/CMakeLists.txt


# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)

######         libusb         ######
set(LIBUSB_DIR ${CMAKE_SOURCE_DIR}/libusb)
set(LIBUSB_SOURCES ${LIBUSB_DIR}/libusb/core.c
        ${LIBUSB_DIR}/libusb/descriptor.c
        ${LIBUSB_DIR}/libusb/hotplug.c
        ${LIBUSB_DIR}/libusb/io.c
        ${LIBUSB_DIR}/libusb/sync.c
        ${LIBUSB_DIR}/libusb/strerror.c
        ${LIBUSB_DIR}/libusb/os/linux_usbfs.c
        ${LIBUSB_DIR}/libusb/os/events_posix.c
        ${LIBUSB_DIR}/libusb/os/threads_posix.c
        ${LIBUSB_DIR}/libusb/os/linux_netlink.c
        )

include_directories(${LIBUSB_DIR}
        ${LIBUSB_DIR}/android
        ${LIBUSB_DIR}/libusb
        ${LIBUSB_DIR}/libusb/os
        )

add_library(libusb STATIC ${LIBUSB_SOURCES})

######         libftdi         ######
set(LIBFTDI_DIR ${CMAKE_SOURCE_DIR}/libftdi)
set(LIBFTDI_SOURCES ${LIBFTDI_DIR}/src/ftdi.c
        ${LIBFTDI_DIR}/src/ftdi_stream.c
        )

include_directories(${LIBFTDI_DIR}
        ${LIBFTDI_DIR}/src
        )

add_library(libftdi STATIC ${LIBFTDI_SOURCES})



# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add_library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
        lolly-backend-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        TMSReader.c)



# Specifies a path to native header files.
include_directories(include/)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        lolly-backend-lib
        libusb
        libftdi
        log)
