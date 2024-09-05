#include "include/TMSReader.h"
#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_tomst_lolly_ui_home_HomeFragment_getExampleStringJNI(JNIEnv *env, jobject thiz) {
    libusb_context *ctx;
    char *hello = "Hello from JNI";
    return (*env)->NewStringUTF(env, hello);
}
//
//static void print_device(libusb_device *dev, libusb_device_handle *handle) {
//    struct libusb_device_descriptor desc;
//    unsigned char string[256];
//    const char *speed;
//    int ret;
//    uint8_t i;
//
//    switch (libusb_get_device_speed(dev)) {
//        case LIBUSB_SPEED_LOW:		speed = "1.5M"; break;
//        case LIBUSB_SPEED_FULL:		speed = "12M"; break;
//        case LIBUSB_SPEED_HIGH:		speed = "480M"; break;
//        case LIBUSB_SPEED_SUPER:	speed = "5G"; break;
//        case LIBUSB_SPEED_SUPER_PLUS:	speed = "10G"; break;
//        default:			speed = "Unknown";
//    }
//
//    ret = libusb_get_device_descriptor(dev, &desc);
//    if (ret < 0) {
//        __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", "failed device descriptor");
//        return;
//    }
//
//    char str[80];
//    sprintf(str, "Dev (bus %u, device %u): %04X - %04X speed: %s\n",
//         libusb_get_bus_number(dev), libusb_get_device_address(dev),
//         desc.idVendor, desc.idProduct, speed);
//
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", str);
//
//    if (!handle) {
//        __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", "no handle, open");
//        libusb_open(dev, &handle);
//    }
//
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", "post no handle check/open");
//
//    if (handle) {
//        if (desc.iManufacturer) {
//            ret = libusb_get_string_descriptor_ascii(handle, desc.iManufacturer, string, sizeof(string));
//            if (ret > 0) {
//                __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", "Manufacturer");
//                __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", (char *) string);
//            }
//        }
//
//        if (desc.iProduct) {
//            ret = libusb_get_string_descriptor_ascii(handle, desc.iProduct, string, sizeof(string));
//            if (ret > 0) {
//                __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", "Product");
//                __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", (char *) string);
//            }
//        }
//
//        if (desc.iSerialNumber) {
//            ret = libusb_get_string_descriptor_ascii(handle, desc.iSerialNumber, string, sizeof(string));
//            if (ret > 0) {
//                __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", "Serial Number");
//                __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG print_device|||", (char *) string);
//            }
//        }
//    }
//}
//
//JNIEXPORT jint JNICALL
//Java_com_tomst_lolly_core_TMSReader_setNativeDescriptor(JNIEnv *env, jobject thiz,
//                                                        jint fileDescriptor) {
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "start");
//    libusb_context *ctx = NULL;
//    libusb_device_handle *devh = NULL;
//    int r = 0;
//
//    struct libusb_init_option optionsArr[] = {
//            {LIBUSB_OPTION_NO_DEVICE_DISCOVERY, 0}
//    };
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "options");
//
//    r = libusb_init_context(&ctx, optionsArr, 1);
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "context_init");
//
////    r = libusb_init(&ctx);
//    if (r < 0) {
//        __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "libusb_init_context failed");
//        return r;
//    }
//
//    r = libusb_wrap_sys_device(ctx, (intptr_t)fileDescriptor, &devh);
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "wrap_sys");
//    if (r < 0) {
//        __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "libusb_wrap_sys_device failed");
//        return r;
//    } else if (devh == NULL) {
//        __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "libusb_wrap_sys_device returned invalid handle");
//        return r;
//    }
//
//    print_device(libusb_get_device(devh), devh);
//
//    libusb_close(devh);
//    libusb_exit(ctx);
//
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG setnative|||", "end");
//
//    return r;
//
//    return -26;
//}
//
//// get device count using libftdi
//JNIEXPORT jint JNICALL
//Java_com_tomst_lolly_core_TMSReader_getDeviceCountC(JNIEnv *env, jobject thiz,
//                                                   jint fileDescriptor) {
//    __android_log_write(ANDROID_LOG_ERROR, "|||DEBUG|||", "start");
//
//    int ret;
//    struct ftdi_context *ftdi;
//
//    ftdi = ftdi_new((intptr_t)fileDescriptor);
//
//    if (ftdi == NULL) {
//        __android_log_write(ANDROID_LOG_ERROR, "TMSReader", "ftdi_new failed");
//
//        return -101;
//    }
//
//    __android_log_write(ANDROID_LOG_ERROR, "||| DEBUG |||", "ftdi_new passed");
//
//    libusb_device *dev = libusb_get_device(ftdi->usb_dev);
//    __android_log_write(ANDROID_LOG_ERROR, "||| DEBUG |||", "after libusb get device");
//
//    ret = ftdi_usb_open_dev(ftdi, dev);
//    __android_log_write(ANDROID_LOG_ERROR, "||| DEBUG |||", "after ftdi usb open dev");
//
//    // cleanup
//    ftdi_free(ftdi);
//
//    return -23;
//}
//
//
//int connectDevice() {
//    return 0;
//}

//int DoInitFTDI() {
//
//}
