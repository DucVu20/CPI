# Camera Interface: OV7670-Chisel Implementation
Targeted camera: OV7670 OmmiVision
# Introduction
This camera interface is designed to integrate on a system on a chip (SoC) on ChipYard framwork to acquire images from OmminiVision OV7670 for a CNN accelerator. However, you can use it for your own needs. The design is written in Chisel and is highly parameterizable. Although the design targets OV7670, it can be used for any camera with the same interface. The design deploys the I2C interface to configure the camera and supports two versions 16 bit RGB formats and RGB888. I recommend you guys to visit the datasheet for more information about the OV7670 camera: http://web.mit.edu/6.111/www/f2016/tools/OV7670_2006.pdf
# Configuration
## SCCB Interface
The OV7670 requires configuration to output the correct color format with the proper color balance. This is done over the SCCB interface, which is a copy of the I2C protocol. Since we're only interested in writing configurations for the camera, the I2C is redesigned only for transmiting data from the interface to the camera. To configure the camera, you must specify the address of a register and its coresponding data, and at insert the *config* signal at the same clock cycle. To disable communication over the I2C interface, write false to the *coreEna* signal. The SCCB interface should be disabled when it's not used
## Capture Module
Since the design is highly parameterizable, you can also configure this module to capture either RGB888 or 16-bit RGB formats. This is dictated by the **bytePerPixel** in the CaptureModule.scala. For example, the OV7670 outputs two bytes of data for a single pixel, thus the **bytePerPixel** must be set to 2 to work with this camera, and 3 for any camera with RGB888. The hardware for RGB888 can also be used to acquire images from 2-byte RGB formats.
Depending on the amount of BRAM avaible on your FPGA board, you can also configure the buffer size to store images via the *bufferDepth* parameter. For example, bufferDepth must be specified to *352x290* to capture any images with sizes less than CIF such as QCIF, QVGA (320x240)... The other two parameters *imgWidthCnt* and *imgHeighCnt* are used to specify the maximum bit width of two counters, namely colCnt, and rowCnt, which return the resolution of a frame transmitted by the interface.
## XCLKGenerator
This block is literally a clock divider, depending on the maximum frequency of your FPGA design, you can divide it to generate expected XCLK for the camera.
# SoC Integration of the interface on ChipYard platfrom
The class for integrating the entire design into a system on chip on Chipyard platform is OV7670.scala. The class for configuring hardware of the CPI is in CPIParams, and configuration, control, status registers are located in CPIMMIO. Sofware file for playing around with the interface is located in /main/src/test/scala/cpi/OV7670.c. To compile the C file for the RISCV based core, run *make OV7670.riscv*.
## MMIO details
![image](https://user-images.githubusercontent.com/63137043/138415144-42276c3b-ba8a-4e48-b5a0-387dff0b14d3.png)
![image](https://user-images.githubusercontent.com/63137043/138415212-ac3ab355-6a2f-47a8-8ad7-1c4d9ffae341.png)
## SOC Demo results
![CIF Resolution](https://github.com/DucVu20/CameraInterface/blob/main/design/CIF_1.png) | ![QCIF Resolution](https://github.com/DucVu20/CameraInterface/blob/main/design/QCIF.png)
# References
[1]Chipyard documentation: https://readthedocs.org/projects/chipyard/downloads/pdf/dev/ <br />
[2] I2C-Master Core Specification - Richard Herveille. Available on https://opencores.org/projects/i2c/downloads <br />
[3]OV7670 datasheet: http://web.mit.edu/6.111/www/f2016/tools/OV7670_2006.pdf <br />
# Contact
gmail: leducvuvietnam@gmail.com <br />
Linkedin: https://www.linkedin.com/in/vudle/
