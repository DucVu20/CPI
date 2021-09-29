#include "mmio.h"
#include "stdio.h"
#include "stdlib.h"

#define CPI_BASE_ADDR                   (0x10020000)
#define CAM_STATUS                      (CPI_BASE_ADDR)
#define CAM_CAPTURE                     (CPI_BASE_ADDR + 0x4)
#define CAM_MODE                        (CPI_BASE_ADDR + 0x8)
#define GRAY_IMAGE                      (CPI_BASE_ADDR + 0xC)

#define RETURN_IMAGE_WIDTH              (CPI_BASE_ADDR + 0x10)
#define RETURN_IMAGE_HEIGHT             (CPI_BASE_ADDR + 0x14)
#define PIXEL                           (CPI_BASE_ADDR + 0x18)
#define PIXEL_ADDR                      (CPI_BASE_ADDR + 0x1C)
#define PRESCALER                       (CPI_BASE_ADDR + 0x20)

// com7 reg: address 0x12
#define REG_COM7    	0x12	/* Control 7 */
#define COM7_RESET  	0x80	/* Register reset */
#define COM7_FMT_MASK	0x38
#define COM7_FMT_VGA	0x00
#define	COM7_FMT_CIF	0x20	/* CIF format */
#define COM7_FMT_QVGA	0x10	/* QVGA format */
#define COM7_FMT_QCIF	0x08	/* QCIF format */
#define	COM7_RGB	    0x04	/* bits 0 and 2 - RGB format */
#define	COM7_YUV	    0x00	/* YUV */


void configure_camera(unsigned int addr, unsigned int config_data){
    unsigned int mode = (addr << 8) | (config_data & 0xFF);
    printf("Camera mode: %08X \n", mode);
    reg_write16(CAM_MODE, mode)   ;   // write configuration to the camera
}

void capture_image() {
    reg_write8(CAM_CAPTURE, 1);
    printf("generated a capture signal an image\n");
}

void check_status(){
  printf("\n");
    if( (reg_read8(CAM_STATUS) & (0x01)) == 1){
      printf("the camera is working\n");
    }else{
       printf("the camera is idle\n");
    }

    if((reg_read8(CAM_STATUS) & (0x2)) == 2){
        printf("buffer is full\n");
    }
    else{
        printf("buffer is empty\n");
    }

    if( (reg_read8(CAM_STATUS) & (0x04)) == 4){
        printf("sccb interface is ready\n");
    }else{
         printf("sccb interface is busy\n");
    }

    printf("\n");
}

int main(void){

    reg_write8(PRESCALER, 4);   // divide clk to create XCLK
    printf("====================================================================");
    check_status();

    printf("Reset the camera ");
    configure_camera(REG_COM7, COM7_RESET) ;   // reset the camera to default mode
    printf("Configured the RGB565 mode \n");
    configure_camera(0x40, 0xD0);   // RGB565
    check_status();
    while((reg_read8(CAM_STATUS) & 0x04) == 0); // wait until the previous mode is configured

    printf("switch to RGB image acquisition mode\n");
    reg_write8(GRAY_IMAGE, 0);

    printf("take the first shot\n ");
    printf("\n");
    capture_image();    // capture image

    while((reg_read8(CAM_STATUS) & 0x01) == 0 ); // wait util the camera is capturing

    check_status();

    //    while((reg_read8(CAM_STATUS) & 0x01) == 1 ){
      //      printf(" Height: %08d\n ", reg_read32(RETURN_IMAGE_HEIGHT));
      //      printf(" Width: %08d\n  ", reg_read32(RETURN_IMAGE_WIDTH));
      //    }
    while((reg_read8(CAM_STATUS) & 0x02) == 0); // wait till the buffer is full
    check_status();

    //===============read pixel from the camera=========//
    while((reg_read8(CAM_STATUS) & 0x02) == 2){
      printf("%04X ", reg_read16(PIXEL));
    }
    printf("the resolution of the first shot is\n");
    printf("Returned image height: %08d\n", reg_read32(RETURN_IMAGE_HEIGHT));
    printf("Returned image width: %08d\n\n", reg_read32(RETURN_IMAGE_WIDTH));
    check_status();

    printf("configure the RGB-CIF resolution 352x288\n");
    configure_camera(REG_COM7, COM7_FMT_CIF);
    while((reg_read8(CAM_STATUS) & 0x04) == 0); // wait until it finishes
    check_status();

    printf("take a second shot\n");
    capture_image();
    while((reg_read8(CAM_STATUS) & 0x02) == 0 ); // wait until the buffer is full
    printf("the resolution of the second shot is\n");
    printf("Returned image height: %08d\n", reg_read32(RETURN_IMAGE_HEIGHT));
    printf("Returned image width: %08d\n", reg_read32(RETURN_IMAGE_WIDTH));
    printf("\n");

    printf("take a third RGB-CIF shot\n");
    capture_image();
    while((reg_read8(CAM_STATUS) & 0x02) == 0 ); // wait until the buffer is full
    printf("the resolution of the third shot is\n");
    printf("Returned image height: %08d\n", reg_read32(RETURN_IMAGE_HEIGHT));
    printf("Returned image width: %08d\n", reg_read32(RETURN_IMAGE_WIDTH));
    printf("\n");

    printf("configure QCIF: 176x144\n");
    configure_camera(REG_COM7, COM7_FMT_QCIF);
    while((reg_read8(CAM_STATUS) & 0x04) == 0); // wait until it finishes

    capture_image();
    printf("take a fourth shot QCIF \n");
    while((reg_read8(CAM_STATUS) & 0x02) == 0 ); // wait until the buffer is full
    printf("the resolution of the fourth shot is\n");
    printf("Returned image height: %08d\n", reg_read32(RETURN_IMAGE_HEIGHT));
    printf("Returned image width: %08d\n", reg_read32(RETURN_IMAGE_WIDTH));
    printf("\n");

    printf("reset to default mode, expected resolution: 640x480\n");
    configure_camera(REG_COM7, COM7_RESET) ;   // reset the camera to default mode
    while((reg_read8(CAM_STATUS) & 0x04) == 0); // wait until the previous mode is configured
    capture_image();
    while((reg_read8(CAM_STATUS) & 0x02) == 0 ); // wait until the buffer is full
    check_status();
    printf("Returned image height: %08d\n", reg_read32(RETURN_IMAGE_HEIGHT));
    printf("Returned image width: %08d\n", reg_read32(RETURN_IMAGE_WIDTH));

}
