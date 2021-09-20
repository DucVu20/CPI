package cpi

import chisel3._
import chisel3.util._

class OV7670Wires(imgWidth: Int, imgHeight: Int,
                  bufferDepth: Int) extends Module{
  val io=IO(new Bundle{
    val pclk         = Input (Bool())
    val href         = Input (Bool())
    val vsync        = Input (Bool())
    val pixelIn      = Input (UInt(8.W))
    val pixelOut     = Output(UInt(16.W))
    val pixelAddr    = Output(UInt(log2Ceil(bufferDepth).W))
    val frameWidth   = Output(UInt(log2Ceil(imgWidth).W))
    val frameHeight  = Output(UInt(log2Ceil(imgHeight).W))
    val imageFormat  = Input(UInt(1.W))                // either RGB or gray
    val capture      = Input (Bool())
    val capturing    = Output(Bool())
    val readFrame    = Input (Bool()) // ready
    val frameFull    = Output(Bool()) // valid
  })
}

class CameraUartTop(CLK_FREQ_MHz: Float, SCCB_FREQ_kHz: Int,
                    imgWidth: Int, imgHeight: Int,
                    bufferDepth: Int, baudRate: Int
                    ) extends  Module {
  val MHz=scala.math.pow(10,6)
  // add modules
//  val captureModuleInterface = Module(new OV7670Wires(imgWidth,
//    imgHeight,bufferDepth))

  var captureModuleInterface = Module(new CaptureModule(imgWidth, imgHeight,
    2, bufferDepth))


  val width = imgWidth
  val height = imgHeight
  val sccb_interface = Module(new SCCBInterface((CLK_FREQ_MHz*MHz).toInt, SCCB_FREQ_kHz))
  val receiver = Module(new Rx_ver1((CLK_FREQ_MHz*MHz).toInt, baudRate))
  val transmitter = Module(new Tx((CLK_FREQ_MHz*MHz).toInt, baudRate))

  val io = IO(new Bundle {
    val rx = Input(UInt(1.W))
    val tx = Output(UInt(1.W))

    val p_clk=Input(Bool())
    val href=Input(Bool())
    val vsync=Input(Bool())
    val pixel_in=Input(UInt(8.W))

    val SIOC = Output(Bool())
    val SIOD = Output(UInt(1.W))

    //delete later
    val valid=Output(Bool())
    val rx_bits=Output(UInt(8.W))
    val cpu_read_data=Output(UInt(8.W))
    val cpu_echo_valid=Output(Bool())
    val cpu_ready=Input(Bool())
  })
  io.rx<>receiver.io.rxd
  io.tx<>transmitter.io.txd
//  /// delete later
  io.valid<>receiver.io.valid
  io.rx_bits<>receiver.io.bits

  val echo_cpu=Module(new Rx((CLK_FREQ_MHz*MHz).toInt, baudRate))
  echo_cpu.io.rxd<>transmitter.io.txd
  echo_cpu.io.channel.bits<>io.cpu_read_data
  echo_cpu.io.channel.valid<>io.cpu_echo_valid
  echo_cpu.io.channel.ready<>io.cpu_ready
//  //delete later

  val idle ::fetch :: Nil = Enum(2)
  val FMS = RegInit(idle)
  val func_field = RegInit(0.U(8.W))
  val addr_field = RegInit(0.U(8.W))
  val data_field = RegInit(0.U(8.W))
  val din_byte_counter = RegInit(0.U(2.W))
  val data_processing_intruction = Cat(func_field, addr_field, data_field)

  //=============== FMS's control signals=================//
  val config = WireInit(false.B)
  val capture = WireInit(false.B)
  val read_image=WireInit(false.B)

  // ==================load data to instruction register===============//
  when(din_byte_counter === 3.U) {
    din_byte_counter := 0.U
  }
  when(receiver.io.valid) {
    din_byte_counter := din_byte_counter + 1.U

    switch(din_byte_counter) {
      is(0.U) {
        func_field := receiver.io.bits
      }
      is(1.U) {
        addr_field := receiver.io.bits
      }
      is(2.U) {
        data_field := receiver.io.bits
      }
      is(3.U) {
        func_field := receiver.io.bits
      } // default if counter to 3
    }
  }

  val tx_data=WireDefault(0.U(8.W))
  val tx_valid=WireDefault(false.B)
  val sccb_status=RegInit(false.B)
  val tx_ready=WireDefault(false.B)
  tx_ready:=transmitter.io.channel.ready
  sccb_status:=sccb_interface.io.sccb_ready
  //==================master FMS========================//
  switch(FMS) {
    is(idle) {
      when(din_byte_counter === 3.U) {
        FMS := fetch // when data processing register is full, execute the instruction
      }
    }
    is(fetch) {
      //FMS := idle
      switch(data_processing_intruction(18, 16)){
        is(0.U){  // check sccb status
          when(tx_ready){
            tx_data:=sccb_status
            FMS:=idle
            tx_valid:=true.B
          }

        }
        is(1.U){  // generate a config signal
          when(sccb_status){
            config:=true.B
            FMS:=idle
          }
        }
        is(2.U){  // capture
          capture:=true.B
          FMS:=idle
        }
        is(3.U){  // check whether the camera is capturing image or not
          when(tx_ready){
            tx_data:=captureModuleInterface.io.capturing
            FMS:=idle
            tx_valid:=true.B
          }
        }
        is(4.U){  //check buffer status
          when(tx_ready){
            tx_valid:=true.B
            tx_data:=captureModuleInterface.io.frameFull
            FMS:=idle
          }
        }
        is(5.U){  // read image
          when(captureModuleInterface.io.frameFull){
            read_image:=true.B
            FMS:=idle
          }
        }
      }
    }
  }
  // remember to taylor reading ports signals
  val read_pixel=WireInit(false.B)
  val byte_counter=RegInit(0.U(1.W))
  val pixel=RegInit(0.U(16.W))
  pixel:=Mux(read_pixel,captureModuleInterface.io.pixelOut,pixel)
  val pixel_byte=Mux(byte_counter===1.U,pixel(7,0),pixel(15,8))
  Mux(read_pixel,captureModuleInterface.io.pixelOut,pixel)    // load new pixel when read_pixel is inserted
                                                        // and increase the address of the next pixel

  val tx_idle::load_pixel:: transmit_to_cpu::waiting::Nil=Enum(4)
  val tx_fms=RegInit(tx_idle)
  switch(tx_fms){
    is(idle){
      when(read_image){
        tx_fms:=load_pixel
      }
    }
    is(load_pixel){
      read_pixel:=true.B
      tx_fms:=transmit_to_cpu
    }
    is(transmit_to_cpu){     // wait until one pixel is transmitted, move to load_pixel
      when(transmitter.io.channel.ready){    // after this, ready goes low
        tx_valid:=true.B
        tx_data:=pixel_byte
        tx_fms:=waiting
        byte_counter:=byte_counter+1.U
      }
    }
    is(waiting){
      when(transmitter.io.channel.ready){
        tx_fms:=Mux(byte_counter.asBool,transmit_to_cpu,load_pixel)
      }
      when(!captureModuleInterface.io.frameFull){ // when empty, done reading
        tx_fms:=idle
      }
    }
  }

  // sccb interface
  sccb_interface.io.control_address:=addr_field
  sccb_interface.io.config_data:=data_field
  sccb_interface.io.config:=config
  io.SIOC:=sccb_interface.io.SIOC
  io.SIOD:=sccb_interface.io.SIOD
  // capture module
  captureModuleInterface.io.readFrame<>read_pixel
  captureModuleInterface.io.pclk<>io.p_clk
  captureModuleInterface.io.href<>io.href
  captureModuleInterface.io.vsync<>io.vsync
  captureModuleInterface.io.pixelIn<>io.pixel_in
  captureModuleInterface.io.capture:=capture
  captureModuleInterface.io.imageFormat := 1.U

  val frameWidth = Wire(UInt(log2Ceil(imgWidth).W))
  val frameHeight = Wire(UInt(log2Ceil(imgHeight).W))
  captureModuleInterface.io.frameWidth <> frameWidth
  captureModuleInterface.io.frameHeight <> frameHeight

  // transmitter
  transmitter.io.channel.valid:=tx_valid // default is false
  transmitter.io.channel.bits:=tx_data
}

object UartCPIVer1 extends App {
  chisel3.Driver.execute(Array[String](), () => new CameraUartTop(50,50,
    640,480,640*480,115200))
}
