package sislab.cpi

//////////////////////////////////////////////////////////////////////////////////
// This module is designed to capture images from OV7670 camera. The designe is //
//                  highly parameterizable and reusable.                        //
//                 Designed and written in Chisel HDL by:                       //
//                                Vu D. Le                                      //
//          at VNU Key Laboratory for Smart Integrated Systems,                 //
//                   Vietnam National University, Hanoi                         //
//                           Time: August 2021                                  //
//////////////////////////////////////////////////////////////////////////////////

//                         Parameter explaination
// imgWidthCnt, and imgHeightCnt: the bit width of the counter that are responsible
// for getting the resolution of an image or video transmitted by the camera
// bytePerPixel: the maximum number of byte per a pixel, 2 for OV7670 and 3 for RGB888
// bufferDepth: the depth or size of the on-chip buffer for storing the images
// transmitted by the camera. For example: to store any images with sizes less than or
//  equal to VGA such as QVGA or CIF, bufferDepth mus be set to 640*480


import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

class CaptureModule(imgWidthCnt: Int, imgHeightCnt: Int,
                    bytePerPixel: Int,
                    bufferDepth: Int)  extends Module {

  val depth     = bufferDepth
  val io  = IO(new Bundle {
    val pclk        = Input(Bool())
    val href        = Input(Bool())
    val vsync       = Input(Bool())
    val pixelIn     = Input(UInt(8.W))
    val pixelOut    = Output(UInt((8*bytePerPixel).W))
    val pixelAddr   = Output(UInt(log2Ceil(bufferDepth).W))
    val frameWidth  = Output(UInt(log2Ceil(imgWidthCnt).W))
    val frameHeight = Output(UInt(log2Ceil(imgHeightCnt).W))
    val RGB888      = if(bytePerPixel == 3) Some(Input(Bool())) else None
    // allow the RGB888 version to capture RGB 16bit images
    val capture     = Input(Bool())
    val videoMode   = Input(Bool())
    val capturing   = Output(Bool())
    val readFrame   = Input(Bool())  // ready
    val pixelValid  = Output(Bool()) // valid
    val frameFull   = Output(Bool()) // interrupt, false  a frame is read out
    val newFrame    = Output(Bool()) // inform a new frame
  })

  val idle :: captureFrame :: Nil = Enum(2)
  val FMS = RegInit(idle)

  val writePtr   = RegInit(0.U(log2Ceil(bufferDepth).W))
  val readPtr    = RegInit(0.U(log2Ceil(bufferDepth).W))
  val firstByte  = RegInit(0.U(8.W))
  val secondByte = RegInit(0.U(8.W))
  val thirdByte  = if (bytePerPixel == 3) Some(RegInit(0.U(8.W))) else None
  val pixel      = if (bytePerPixel == 2) Some(Cat(firstByte, secondByte))
                            else Some(Cat(firstByte, secondByte, thirdByte.get))

  val captureSignalHolder = RegInit(false.B)  // used to hold the capture event
  // signal to capture an image when signals for a new frame is available
  val bufferDepthCounter  = RegInit(0.U(log2Ceil(bufferDepth).W))
  val newFrame            = RegInit(false.B)
  val pixelIndex          = RegInit(0.U(log2Ceil(bytePerPixel).W))
  val rowCnt              = RegInit(0.U(log2Ceil(imgHeightCnt).W))
  val colCnt              = RegInit(0.U(log2Ceil(imgWidthCnt).W))
  val frameFull           = RegInit(false.B) // buffer status register
  val bufferOverflow      = RegInit(false.B)
  val capturing           = WireInit(false.B)

  val bufferAddr  = WireInit(0.U(log2Ceil(bufferDepth).W))
  val wrEnaWire   = WireInit(false.B)
  val wrEna       = RegNext(wrEnaWire)
  val pixelValid  = RegInit(false.B)

  val buffer = Module(new SinglePortRam(bufferDepth,UInt((8*bytePerPixel).W)))

  val vsyncFallingEdge = (!io.vsync) &&   RegNext(io.vsync)
  val hrefFallingEdge  = (!io.href)  &&   RegNext(io.href)
  val hrefRisingEdge   =   io.href   && (!RegNext(io.href))
  val pclkRisingEdge   =   io.pclk   &  (!RegNext(io.pclk))
  //=====================READ ADDRESS GENERATOR==================//
  // allow reading pixels from the buffer while writePtr is not writing//
  when( (io.videoMode || io.readFrame) && frameFull ) {
    when(capturing && (!io.href)){
      bufferAddr := readPtr
      readPtr    := readPtr + 1.U
      pixelValid := true.B
    }.elsewhen( !capturing){
      bufferAddr := readPtr
      readPtr    := readPtr + 1.U
      pixelValid := true.B
    }.otherwise{
      pixelValid := false.B
    }
    when(readPtr === bufferDepthCounter) {
      readPtr            := 0.U
      bufferDepthCounter := 0.U
      frameFull          := false.B
      pixelValid         := false.B
    }
  }.otherwise {
    bufferAddr := RegNext(writePtr)
  }

  captureSignalHolder  := Mux(io.capture, io.capture, captureSignalHolder)
  //====================FMS for capturing images============================//
  switch(FMS) {
    is(idle){
      when(io.vsync) {
        FMS := idle
      } otherwise {
        when(captureSignalHolder | io.videoMode) {
          when(vsyncFallingEdge) {
            FMS                 := captureFrame
            captureSignalHolder := false.B
            newFrame            := false.B
            rowCnt              := 0.U
            colCnt              := 0.U   // reset counters
            writePtr            := 0.U
          }
        }
      }
      capturing := false.B
    }
    is(captureFrame) {
      capturing := true.B
      newFrame  := Mux(io.vsync, true.B, false.B)
      FMS       := Mux(io.vsync, idle, captureFrame)

      when(io.href && pclkRisingEdge) {
        if (bytePerPixel == 2) {   // for 16 bit rgb version
          pixelIndex := !pixelIndex
          switch(pixelIndex) {
            is(0.U) {
              firstByte := io.pixelIn
            }
            is(1.U) {
              secondByte := io.pixelIn
              wrEnaWire := pclkRisingEdge
            }
          }
        }
        else if (bytePerPixel == 3) { // for 24 bit rgb version
          pixelIndex := pixelIndex + 1.U
          when(io.RGB888.get) {
            when(pixelIndex === 2.U) {
              pixelIndex := 0.U
            }
            switch(pixelIndex) {
              is(0.U) {
                firstByte  := io.pixelIn
              }
              is(1.U) {
                secondByte := io.pixelIn
              }
              is(2.U) {
                thirdByte.get := io.pixelIn
                wrEnaWire     := pclkRisingEdge
              }
            }
          }.otherwise {
            when(pixelIndex === 1.U) {
              pixelIndex := 0.U
            }
            firstByte := 0.U
            switch(pixelIndex) {
              is(0.U) {
                secondByte := io.pixelIn
              }
              is(1.U) {
                thirdByte.get := io.pixelIn
                wrEnaWire     := pclkRisingEdge
              }
            }
          }
        }
        else None
      }
      when(wrEnaWire){  // increase counter when write to mem
        colCnt     := colCnt + 1.U
        writePtr   := writePtr + 1.U
      }
      when(hrefRisingEdge){
        rowCnt := rowCnt + 1.U
        colCnt := 0.U
      }
    }
  }
  when(newFrame) {
    bufferDepthCounter := writePtr  // update the number of pixel written to the buffer
  }
  when(newFrame & (!RegNext(newFrame))){ // update frameFull with newFrame
    frameFull          := true.B
  }
  //=======================connect signals to the buffer========================//
  buffer.io.wrEna   := wrEna
  buffer.io.rdEna   := io.readFrame
  buffer.io.addr    := bufferAddr
  buffer.io.dataIn  := pixel.get

  //==============================IO============================================//
  io.capturing      := capturing
  io.frameWidth     := colCnt
  io.frameHeight    := rowCnt
  io.pixelAddr      := RegNext(readPtr)
  io.pixelOut       := buffer.io.dataOut
  io.newFrame       := newFrame
  io.frameFull      := frameFull
  io.pixelValid     := pixelValid
}

object CaptureModule extends App{
  new (ChiselStage).emitVerilog(new CaptureModule(CPIParams.apply().imgWidthCnt, CPIParams.apply().imgHeightCnt,
    CPIParams.apply().bytePerPixel, CPIParams.apply().bufferDepth), Array("--target-dir","generatedVerilog"))
}
