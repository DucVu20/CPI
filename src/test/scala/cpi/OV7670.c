#include "mmio.h"
#include "stdio.h"
#include "stdlib.h"

#define CPI_BASE_ADDR                   (0x10020000)
#define CAM_STATUS                      (CPI_BASE_ADDR)
#define CAM_CAPTURE                     (CPI_BASE_ADDR + 0x4)
#define CAM_MODE                        (CPI_BASE_ADDR + 0x8)
#define CAM_CONFIG                      (CPI_BASE_ADDR + 0xC)
#define IMAGE_FORMAT                    (CPI_BASE_ADDR + 0x14)

#define RETURNED_IMAGE_RESOLUTION       (CPI_BASE_ADDR + 0x18)
#define PIXEL                           (CPI_BASE_ADDR + 0x1C)
#define PRESCALER                       (CPI_BASE_ADDR + 0x20)

// com7 reg: address 0x12
#define SSCB_REG_RESET_ALL (1 << 7)
#define OUTPUT_QCIF (1 << 3)
#define OUTPUT_QVGA (1 << 4)
#define OUTPUT_CIF (1 << 5)
#define OUTPUT_RGB (1 << 2)
#define OUTPUT_RAW (1)

void configure_camera(unsigned int addr, unsigned int config_data){
    unsigned int mode = (addr << 8) | (config_data & 0xFF);
    printf("Camera mode: %08X ADDR: %08X DATA: %08X\n", mode, addr, config_data);
    reg_write16(CAM_MODE, mode)   ;   // write configuration to the camera
    reg_write8(CAM_CONFIG, 1)    ;    // write one to force the camera to
                                     // work at the predefined mode
    printf("Configuration mode transmitted\n");
}
void capture_image() {
    reg_write32(CAM_CAPTURE, 1);
    printf("generated a capture signal an image\n");
    reg_write32(CAM_CAPTURE, 0);
}
void check_status(){
    if((reg_read8(CAM_STATUS) & (0x2)) == 2){
            printf("buffer is full\n");
    }
    else{
        printf("buffer is empty\n");
    }
    if( (reg_read8(CAM_STATUS) & (0x04)) == 4){
        printf("sccb interface is ready to configure a new mode\n");
    }else{
         printf("sccb interface is configuring a mode to the camera\n");
    }
}
int main(void){

    reg_write8(PRESCALER, 4);

    check_status();

    while((reg_read8(CAM_STATUS) & 0x04) == 0);
    unsigned int mode = SSCB_REG_RESET_ALL | OUTPUT_RGB | OUTPUT_QCIF;
    configure_camera(0x12, mode) ;   // reset the camera to default mode

    printf("Reset the camera \n");
    while((reg_read8(CAM_STATUS) & 0x04) == 0);        // wait until sccb interface is ready

    configure_camera(0x40, 0xD0);   // RGB565
    printf("Configured the RGB565 mode \n");

    reg_write8(IMAGE_FORMAT, 1);
    printf("the interface will capture RGB image\n");

    capture_image();

    while((reg_read8(CAM_STATUS) & 0x02) != 2);
    printf("Buffer is full\n");
    for(int i = 0; i < 176*144*2; ++i){
      printf("%04X ", reg_read16(PIXEL));
      if (i % 8 == 0) printf("\n");
    }
    printf("\n");
    printf("Returned Resolution: %08X\n", reg_read32(RETURNED_IMAGE_RESOLUTION));
    check_status();
}