package sislab.cpi

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import chisel3.iotesters._
import org.scalatest._


case class params(
                   systemFreqMHz: Float,
                   sccbFreqHz: Int,
                   imgWidth: Int,
                   imgHeight: Int,
                   bufferDepth: Int,
                   baudRate: Int,
                   test: Boolean= true
                 )

class CameraUartTop(p: params) extends  Module {
  val MHz    = scala.math.pow(10,6)
  val width  = p.imgWidth
  val height = p.imgHeight

  var CPI = Module(new CaptureModule(p.imgWidth, p.imgHeight, 2, p.bufferDepth))
  val sccbInterface = Module(new SCCBInterface((p.systemFreqMHz*MHz).toInt, p.sccbFreqHz))
  val receiver      = Module(new Rx_ver1((p.systemFreqMHz*MHz).toInt, p.baudRate))
  val transmitter   = Module(new Tx((p.systemFreqMHz*MHz).toInt, p.baudRate))
  //dontTouch(CPI)

  val io = IO(new Bundle {
    val rx = Input(UInt(1.W))
    val tx = Output(UInt(1.W))

    val pclk     = Input(Bool())
    val href     = Input(Bool())
    val vsync    = Input(Bool())
    val pixelIn = Input(UInt(8.W))

    val SIOC = Output(Bool())
    val SIOD = Output(UInt(1.W))

    //delete this part if implement on fpga
    val valid         = if (p.test) Some(Output(Bool())) else None
    val rxBits        = if (p.test) Some(Output(UInt(8.W))) else None
    val cpuReadData   = if (p.test) Some(Output(UInt(8.W))) else None
    val cpuEchoValid  = if (p.test) Some(Output(Bool())) else None
    val cpuReady      = if (p.test) Some(Input(Bool())) else None

  })
  io.rx<>receiver.io.rxd
  io.tx<>transmitter.io.txd

  if(p.test){
    io.valid.get     <> receiver.io.valid
    receiver.io.bits <> io.rxBits.get

    val echo_cpu=Module(new Rx((p.systemFreqMHz*MHz).toInt, p.baudRate))
    echo_cpu.io.rxd           <> transmitter.io.txd
    echo_cpu.io.channel.bits  <> io.cpuReadData.get
    echo_cpu.io.channel.valid <> io.cpuEchoValid.get
    echo_cpu.io.channel.ready <> io.cpuReady.get
  }

  val idle ::fetch :: Nil = Enum(2)
  val FMS = RegInit(idle)
  val funcField = RegInit(0.U(8.W))
  val addrField = RegInit(0.U(8.W))
  val dataField = RegInit(0.U(8.W))
  val dinByteCounter           = RegInit(0.U(2.W))
  val dataProcessingIntruction = Cat(funcField, addrField, dataField)

  //=============== FMS's control signals=================//
  val config    = WireInit(false.B)
  val capture   = WireInit(false.B)
  val readImage = WireInit(false.B)

  // ==================load data to instruction register===============//
  when(dinByteCounter === 3.U) {
    dinByteCounter := 0.U
  }
  when(receiver.io.valid) {
    dinByteCounter := dinByteCounter + 1.U

    switch(dinByteCounter) {
      is(0.U) {
        funcField := receiver.io.bits
      }
      is(1.U) {
        addrField := receiver.io.bits
      }
      is(2.U) {
        dataField := receiver.io.bits
      }
      is(3.U) {
        funcField := receiver.io.bits
      } // default if counter to 3
    }
  }

  val txData     = WireDefault(0.U(8.W))
  val txValid    = WireDefault(false.B)
  val sccbStatus = RegInit(false.B)
  val txReady    = WireDefault(false.B)
  txReady    := transmitter.io.channel.ready
  sccbStatus := sccbInterface.io.sccbReady
  //==================master FMS========================//
  switch(FMS) {
    is(idle) {
      when(dinByteCounter === 3.U) {
        FMS := fetch // when data processing register is full, execute the instruction
      }
    }
    is(fetch) {
      //FMS := idle
      switch(dataProcessingIntruction(18, 16)){
        is(0.U){  // check sccb status
          when(txReady){
            txData  := sccbStatus
            FMS     := idle
            txValid := true.B
          }

        }
        is(1.U){  // generate a config signal
          when(sccbStatus){
            config := true.B
            FMS    := idle
          }
        }
        is(2.U){  // capture
          capture := true.B
          FMS     := idle
        }
        is(3.U){  // check whether the camera is capturing image or not
          when(txReady){
            txData  := CPI.io.capturing
            FMS     := idle
            txValid := true.B
          }
        }
        is(4.U){  //check buffer status
          when(txReady){
            txValid := true.B
            txData  := CPI.io.frameFull
            FMS     := idle
          }
        }
        is(5.U){  // read image
          when(CPI.io.frameFull){
            readImage := true.B
            FMS       := idle
          }
        }
      }
    }
  }
  // remember to taylor reading ports signals

  val read         = WireInit(false.B)
  val loadPixel    = WireInit(false.B)
  val byte_counter = RegInit(0.U(1.W))
  val pixel        = RegInit(0.U(16.W))
  pixel := Mux(loadPixel, CPI.io.pixelOut, pixel)
  val pixel_byte = Mux(byte_counter===1.U, pixel(7,0), pixel(15,8))

  val fmsIdle::fmsReadPixel::fmsLoadPixel:: fmsTransmitToCpu::fmsTransmitting::Nil=Enum(5)
  val txFms=RegInit(fmsIdle)
  switch(txFms){
    is(idle){
      when(readImage){
        txFms := fmsReadPixel
      }
    }
    is(fmsReadPixel){
      read  := true.B
      txFms := fmsLoadPixel
    }
    is(fmsLoadPixel){
      loadPixel := true.B
      txFms     := fmsTransmitToCpu

    }
    is(fmsTransmitToCpu){     // wait until one pixel is transmitted, move to load_pixel
      when(transmitter.io.channel.ready){    // after this, ready goes low
        txValid := true.B
        txData  := pixel_byte
        txFms   := fmsTransmitting
        byte_counter := byte_counter + 1.U
      }
    }
    is(fmsTransmitting){
      when(transmitter.io.channel.ready){
        txFms := Mux(byte_counter.asBool, fmsTransmitToCpu, fmsReadPixel)
      }
      when(!CPI.io.frameFull){ // when empty, load the final pixel before
        // returning back to idle state
        txFms := Mux(byte_counter.asBool(), fmsTransmitToCpu, idle)
      }
    }
  }

  // sccb interface
  sccbInterface.io.controlAddress := addrField
  sccbInterface.io.configData     := dataField
  sccbInterface.io.config         := config
  io.SIOC := sccbInterface.io.SIOC
  io.SIOD := sccbInterface.io.SIOD
  // capture module
  CPI.io.readFrame := read
  CPI.io.pclk      := io.pclk
  CPI.io.href      := io.href
  CPI.io.vsync     := io.vsync
  CPI.io.pixelIn   := io.pixelIn
  CPI.io.capture   := capture
  CPI.io.grayImage := false.B

  val frameWidth = Wire(UInt(log2Ceil(p.imgWidth).W))
  val frameHeight = Wire(UInt(log2Ceil(p.imgHeight).W))
  frameWidth  := CPI.io.frameWidth
  frameHeight := CPI.io.frameHeight
  // transmitter
  transmitter.io.channel.valid:=txValid // default is false
  transmitter.io.channel.bits:=txData
}

object CameraUartTop extends App{
  new (ChiselStage).emitVerilog(new CameraUartTop(params.apply(50.toFloat,
    50, 320, 240, 320*240, 115200, false)), Array("--target-dir","generated"))
}