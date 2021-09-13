#include "mmio.h"
#include "stdio.h"
#include "stdlib.h"

#define CAM_STATUS 0x10003000
#define CAM_CAPTURE 0x10003004
#define CAM_MODE 0x10003008
#define CAM_CONFIG 0x1000300C
#define RETURNED_IMAGE_RESOLUTION 0x10003010
#define PIXEL 0x10003014
#define PRESCALER 0x10003018
#define SIOC_SIOD 0x1000301C


void configure_camera(unsigned int addr, unsigned int config_data){
    unsigned int mode=(addr<<8)||(config_data);
    reg_write16(CAM_MODE, mode)   ;   // write configuration to the camera
    reg_write8(CAM_CONFIG, 1)    ;    // write one to force the camera to
                                     // work at the predefined mode
    printf("Configuration mode transmitted\n");
}
void capture_image() {
    reg_write32(CAM_CAPTURE, 1);
    printf("Activate the camera to capture image\n");
}
void check_status(){
    if((reg_read8(CAM_STATUS)&(0x2))==2){
            printf("buffer is full\n");
    }
    else{
        printf("buffer is empty\n");
        }
    if((reg_read8(CAM_STATUS)&(0x04))==4){
        printf("sccb interface is ready to configure a new mode\n");
    }else{
         printf("sccb interface is configuring a mode to the camera\n");
    }

}

int main(void){

    reg_write8(PRESCALER, 4);

    check_status();

    while((reg_read8(CAM_STATUS)&0x04)==0);
    configure_camera( 0x12, 0x80) ;   // reset the camera to default mode

    printf("Reset the camera \n");
    while((reg_read8(CAM_STATUS)&0x04)==0);        // wait until sccb interface is ready

    configure_camera(0x40, 0xD0);   // RGB565
    printf("Configured the RGB565 mode \n");
    capture_image();
    printf("generated a capture signal an image \n");

}