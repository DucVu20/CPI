package CPI.DualClockCPI

import CPI.clockDivider
import chisel3._
import chisel3.util._

class CaptureModuleDualClock(imgWidth: Int, imgHeight: Int) extends Module{

  val w = imgWidth
  val h = imgHeight
  val bufferDepth = imgWidth * imgHeight // the maximum depth of the buffer
  val pixelBits = 16

  val io  = IO(new Bundle {
    val pclk         = Input (Clock())
    val href          = Input (Bool())
    val vsync         = Input (Bool())
    val pixelIn       = Input (UInt(8.W))

    val pixelOut      = Output(UInt(pixelBits.W))
    val pixelAddr     = Output(UInt(log2Ceil(bufferDepth).W))
    val frameWidth    = Output(UInt(log2Ceil(640).W))
    val frameHeight   = Output(UInt(log2Ceil(480).W))
    val imageFormat   = Input(UInt(1.W))
    val capture       = Input (Bool())
    val capturing     = Output(Bool())
    val read_frame    = Input (Bool()) // ready
    val bufferStatus  = Output(Bool()) // valid
    val frameDone     = Output(Bool())
  })

  val dualClockBuffer    = Module(new DualClockRam(bufferDepth,UInt(pixelBits.W)))
  val captureInterface = Module(new CaptureInterface(bufferDepth))

  val bufferStatus        = RegInit(false.B)
  val readPtr             = RegInit(0.U(log2Ceil(bufferDepth).W))
  val bufferDepthCounter  = RegInit(0.U(log2Ceil(bufferDepth).W))
  val captureSignalHolder = RegInit(false.B)

  //============keep the capture signal until href is available=========//
  val sample :: keep::Nil=Enum(2)

  val captureSignalFMS = RegInit(keep)
  switch(captureSignalFMS){
    is(sample){
      captureSignalHolder    := Mux(io.capture, io.capture, captureSignalHolder)

      when(captureInterface.io.FMS.asBool){
        captureSignalHolder := false.B
        captureSignalFMS := keep
      }
    }
    is(keep){
      captureSignalHolder    := Mux(io.capture, io.capture, captureSignalHolder)
      when(captureInterface.io.FMS===0.U){
        captureSignalFMS := sample
      }
    }
  }
  //=====================READ ADDRESS GENERATOR==================//
  when(io.read_frame) {
    readPtr    := readPtr + 1.U
    when(readPtr === (bufferDepthCounter - 1.U)) {
      readPtr            := 0.U
      bufferStatus       := false.B
      bufferDepthCounter := 0.U
    }
  }
  //====================status register=====================//
  val bufferStatusFMS=RegInit(sample)
  switch(bufferStatusFMS){
    is(sample){
      when(captureInterface.io.frameDone){
        bufferStatus    := captureInterface.io.frameDone
        bufferStatusFMS := keep
      }
    }
    is(keep){
      when((!captureInterface.io.frameDone)){
        bufferStatusFMS :=sample
      }
    }
  }

  //===================capture interface - buffer wire connection==================/
  when((captureInterface.io.frameDone)){
    bufferDepthCounter := captureInterface.io.pixelAddress
  }

  dualClockBuffer.io.writeClock := io.pclk
  dualClockBuffer.io.dataIn     := captureInterface.io.pixelOut
  dualClockBuffer.io.readAddr   := readPtr
  dualClockBuffer.io.wrAddr     := captureInterface.io.pixelAddress
  dualClockBuffer.io.wrEna      := captureInterface.io.pixelValid
  dualClockBuffer.clock         := clock


  io.pixelOut  := (dualClockBuffer.io.dataOut)

  //========================capture interface - IO signals===========================//
  captureInterface.io.pclk        := io.pclk
  captureInterface.io.href        := io.href
  captureInterface.io.vsync       := io.vsync
  captureInterface.io.capture     := captureSignalHolder
  captureInterface.io.pixelIn     := io.pixelIn
  captureInterface.io.imageFormat := io.imageFormat

  io.capturing   := captureInterface.io.capturing
  io.frameWidth  := captureInterface.io.frameWidth
  io.frameHeight := captureInterface.io.frameHeight
  io.frameDone   := captureInterface.io.frameDone

  io.pixelAddr    := RegNext(RegNext(readPtr))      // optional, generate to ease verifications
  io.bufferStatus := bufferStatus

}

//========================DEMO CAPTURE MODULE DUAL CLOCK DEMO========================//
class CaptureModuleDualClockDemo(width: Int,
                                 height: Int,
                                 max_prescaler: Int) extends Module {
  val io = IO(new Bundle {
    val pclk         = Input (Clock())
    val href          = Input (Bool())
    val vsync         = Input (Bool())
    val pixelIn       = Input (UInt(8.W))

    val pixelOut      = Output(UInt(16.W))
    val pixelAddr     = Output(UInt(log2Ceil(width*height).W))
    val frameWidth    = Output(UInt(log2Ceil(640).W))
    val frameHeight   = Output(UInt(log2Ceil(480).W))
    val imageFormat   = Input(UInt(1.W))
    val capture       = Input (Bool())
    val capturing     = Output(Bool())
    val read_frame    = Input (Bool()) // ready
    val bufferStatus  = Output(Bool()) // valid
    val frameDone     = Output(Bool())
    val prescaler     = Input(UInt(log2Ceil(max_prescaler).W))
  })

  val clock_div = Module(new clockDivider(max_prescaler))
  val capture_module = Module(new CaptureModuleDualClock(width,height))

  clock_div.io.clock_in  := clock
  clock_div.io.reset     := reset
  clock_div.io.prescaler := io.prescaler

  capture_module.io.pclk        <> clock_div.io.divided_clock
  capture_module.io.href        <> io.href
  capture_module.io.vsync       <> io.vsync
  capture_module.io.pixelIn     <> io.pixelIn
  capture_module.io.pixelOut    <> io.pixelOut
  capture_module.io.frameDone   <> io.frameDone
  capture_module.io.frameWidth  <> io.frameWidth
  capture_module.io.frameHeight <> io.frameHeight
  capture_module.io.imageFormat <> io.imageFormat
  capture_module.io.capture     <> io.capture
  capture_module.io.capturing   <> io.capturing

  io.pixelOut     <> capture_module.io.pixelOut
  io.pixelAddr    <> capture_module.io.pixelAddr
  io.read_frame   <> capture_module.io.read_frame
  io.bufferStatus <> capture_module.io.bufferStatus
}
