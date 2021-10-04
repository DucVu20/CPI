#include "mmio.h"
#include "stdio.h"
#include "stdlib.h"

#define CPI_BASE_ADDR                   (0x10020000)
#define INTERFACE_SETUP                 (CPI_BASE_ADDR)
#define INTERFACE_STATUS                (CPI_BASE_ADDR + 0x04)
#define XCLK_PRESCALER                  (CPI_BASE_ADDR + 0x08)
#define SCCB_DATA                       (CPI_BASE_ADDR + 0xC)
#define CAPTURE                         (CPI_BASE_ADDR + 0x10)

#define RETURN_IMAGE_WIDTH              (CPI_BASE_ADDR + 0x14)
#define RETURN_IMAGE_HEIGHT             (CPI_BASE_ADDR + 0x18)
#define PIXEL                           (CPI_BASE_ADDR + 0x1C)
#define PIXEL_ADDR                      (CPI_BASE_ADDR + 0x20)
#define I2C_PRESCALER_LOW               (CPI_BASE_ADDR + 0x24)
#define I2C_PRESCALER_HIGH              (CPI_BASE_ADDR + 0x28)


// com7 reg: address 0x12
#define REG_COM7    	0x12	/* Control 7 */
#define COM7_RESET  	0x80	/* Register reset */
#define COM7_FMT_MASK	0x38
#define COM7_FMT_VGA	0x00
#define	COM7_FMT_CIF	0x20	/* CIF format */
#define COM7_FMT_QVGA	0x10	/* QVGA format */
#define COM7_FMT_QCIF	0x08	/* QCIF format */
#define	COM7_RGB        0x04	/* bits 0 and 2 - RGB format */
#define	COM7_YUV        0x00	/* YUV */
// com15 reg: address 0x40
#define REG_COM15	0x40  /* Control 15 */
#define COM15_R10F0     0x00  /* Data range 10 to F0 */
#define COM15_R01FE     0x80  /*      01 to FE */
#define COM15_R00FF     0xc0  /*      00 to FF */
#define COM15_RGB565    0x10  /* RGB565 output */
#define COM15_RGB555    0x30  /* RGB555 output */
// CPI interface setup register
#define ACTIVATE_XCLK   0x01
#define I2C_CORE_ENA 0x02
#define VIDEO_MODE 0x04
#define RGB888 0x08 /* if configure hardware for RGB888 */

// CPI status register:
#define CAM_CAPTURING 0x01
#define NEW_FRAME 0x02
#define FRAME_FULL 0x04
#define SCCB_READY 0x08
#define VIDEO_OR_CAPTURE 0x10

volatile int interface_status, return_image_width, return_image_height;



void capture_image() {
    reg_write8(CAPTURE, 1);
    printf("generated a capture signal\n");
}

void check_status(){
    printf("\n");
    interface_status = reg_read8(INTERFACE_STATUS);
    if( (interface_status & 0x01) == 1){
      printf("the camera is working\n");
    }else{
       printf("the camera is idle\n");
    }

    if( (interface_status & 0x02) == 2){
        printf("a new frame has been captured\n");
     }

    if( (interface_status & 0x04) == 4){
        printf("sccb interface is ready\n");
    }else{
         printf("sccb interface is busy\n");
    }
    if( (interface_status & 0x10) == 16){
      printf("the interface is in video mode");
    }else{
      printf("the interface is in capture mode");
    }
    printf("\n");
}
void configure_camera(unsigned int addr, unsigned int config_data){

    unsigned int mode = (addr << 8) | (config_data & 0xFF);
    while((reg_read8(INTERFACE_STATUS) & 0x08) == 0); // wait until ready
    reg_write16(SCCB_DATA, mode)   ;      // write configuration to the camera
    check_status();
    while((reg_read8(INTERFACE_STATUS) & 0x8) == 0); // wait until finished
    printf("Camera mode: %08X \n", mode);
}
void check_resolution(){
    return_image_width  = reg_read16(RETURN_IMAGE_WIDTH);
    return_image_height = reg_read16(RETURN_IMAGE_HEIGHT);
    printf("Returned image height: %08d", return_image_height);
    printf(", width: %08d\n", return_image_width);
    printf("\n");
}
void read_frame(){
    interface_status = reg_read8(INTERFACE_STATUS);
    while((interface_status & 0x02) == 2){
        printf("%04X ",reg_read16(PIXEL));
    }
}

void wait_frame(){
    while((reg_read8(INTERFACE_STATUS) & 0x02) == 0);
    printf("new frame \n");
}
void delay_ms(char ms){
  for(char n = 0; n < ms; n++){
    for(int cycle = 0; cycle <50000; cycle++){
    }
  }
}

int main(void){
    printf("====================================================================");
    check_status();
    reg_write8(I2C_PRESCALER_LOW, 99); // generate 100k SIOC
    reg_write8(I2C_PRESCALER_HIGH, 0); 
    reg_write8(XCLK_PRESCALER,4); // generate 12.5 MHz XCLK
    reg_write8(INTERFACE_SETUP, 3); // activate XCLK, SCCB, config for capturing rgb
    printf("Reset the camera ");
    configure_camera(REG_COM7, COM7_RESET) ;   // reset the camera to default mode
    delay_ms(1);
    capture_image();
    while((reg_read8(INTERFACE_STATUS) & 0x02) == 0);
    check_resolution();
    //    printf("Configured the RGB565 mode \n");
    //    configure_camera(0x40, 0xD0);   // RGB565
    printf("Configure YUV mode");
    configure_camera(REG_COM7, COM7_YUV);
    check_status();

    printf("take the first shot\n ");
    printf("\n");
    capture_image();

   while((reg_read8(INTERFACE_STATUS) & 0x01) == 0 ); // wait util the camera is capturing
    check_status();

    while((reg_read8(INTERFACE_STATUS) & 0x02) == 0); //wait until sccb ready
    check_status();
    check_resolution();

    printf("configure the RGB-CIF resolution 352x288\n");
    configure_camera(REG_COM7, COM7_FMT_CIF);

    printf("take a second shot\n");
    capture_image();
    while((reg_read8(INTERFACE_STATUS) & 0x02) == 0); // wait for new frame
    check_resolution();

    printf("take a third RGB-CIF shot\n");
    capture_image();
    while((reg_read8(INTERFACE_STATUS) & 0x02) == 0);
    check_resolution();

    //    printf("configure QCIF: 176x144\n");
    //    configure_camera(REG_COM7, COM7_FMT_QCIF);
    //    capture_image();
    //    while((reg_read8(INTERFACE_STATUS) & 0x02) == 0); // wait new frame
    //    check_resolution();

    //capture 60 frames and only returns the resolution
    for(int a =0; a<60; a++){
      capture_image();
      while((reg_read8(INTERFACE_STATUS) & 0x02) == 0); // wait for new frame
      printf("frame %d: ", a);
      check_resolution();
    }
    while((reg_read8(INTERFACE_STATUS) & 0x02) == 0);
    check_status();
    if((reg_read16(RETURN_IMAGE_WIDTH)==352) & (reg_read16(RETURN_IMAGE_HEIGHT) == 290)){
      while((reg_read8(INTERFACE_STATUS) & 0x02) == 2){
	printf("%04X ", reg_read16(PIXEL)&0x00FF);  
      }
    }
    reg_write8(INTERFACE_SETUP, 0); // shut the interface
}
//make PROGRAM=OV7670 TARGET=chipyard-arty-ddr CONFIGURATION=debug LINK_TARGET=ramrodata clean
