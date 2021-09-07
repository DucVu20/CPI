package OV7670_chisel
import OV7670_chisel.Types._
import chisel3._
import chisel3.util._


/**
  The design for the following module is dictated as followed
  First, a host computer sends the commands to be executed, followed by the
  address byte, the data byte
  This design features 4 function codes for data execution
  Function code
        000: configure the camera, a full 3 byte frame need to be send to configure
              the camera correctly
        001: capture image, send this command to capture image
        010: check image status, check if an image is captured
        011: Read image, send this command to read image to a host computer
  data_processing_intruction(16,17) 18-23 are reserved
 */

// in this design, 2 bit function code is used
object Types{
  val configure_camera::capture_image::image_status::read_image::Nil=Enum(4)
}

class camera_top(CLK_FREQ: Int,SCCB_FREQ: Int,
                 img_width: Int, img_height: Int,
                 byte_per_pixel: Int,
                 baudRate: Int) extends  Module {
  // add modules
  val frame_capture = Module(new capture_frame_pixel_ver(img_width, img_height, byte_per_pixel))
  val sccb_interface = Module(new SCCB_interface(CLK_FREQ, SCCB_FREQ))
  val receiver = Module(new Rx_ver1(CLK_FREQ, baudRate))
  val transmitter = Module(new Tx(CLK_FREQ, baudRate))

  val io = IO(new Bundle {
    val rx = Input(UInt(1.W))
    val tx = Output(UInt(1.W))

    val p_clk=Input(Bool())
    val href=Input(Bool())
    val vsync=Input(Bool())
    val data_in=Input(UInt(8.W))
    //val frame_done=Output(Bool())

    val SIOC = Output(Bool())
    val SIOD = Output(UInt(1.W))
  })
  io.rx<>receiver.io.rxd
  io.tx<>transmitter.io.txd

  val idle ::fetch :: Nil = Enum(2)
  val FMS = RegInit(idle)
  val func_field = RegInit(0.U(8.W))
  val addr_field = RegInit(0.U(8.W))
  val data_field = RegInit(0.U(8.W))
  val din_byte_counter = RegInit(0.U(2.W))
  val data_processing_intruction = Cat(func_field, addr_field, data_field)

  //=============== FMS's control signals=================//
  val start = WireInit(false.B)
  val capture = WireInit(false.B)
  val read_image=WireInit(false.B)

  // ==================load data to instruction registion===============//
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
  //==================master FMS========================//
  switch(FMS) {
    is(idle) {
      when(din_byte_counter === 3.U) {
        FMS := fetch // when data processing register is full, execute the instruction
      }
    }
    is(fetch) {
      //FMS := idle
      switch(data_processing_intruction(17, 16)){
        is(0.U){  //config
          when(sccb_interface.io.ready){
            start:=true.B
            FMS:=idle
          }
        }
        is(1.U){  // capture
          capture:=true.B
          FMS:=idle
        }
        is(2.U){  //check status
          when(transmitter.io.channel.ready){
            tx_valid:=true.B
            tx_data:=frame_capture.io.buffer_full
            FMS:=idle
          }
        }
        is(3.U){  // read image
          when(frame_capture.io.buffer_full){
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
  pixel:=Mux(read_pixel,frame_capture.io.data_out,pixel)
  val pixel_byte=Mux(byte_counter===1.U,pixel(15,8),pixel(7,0))
  Mux(read_pixel,frame_capture.io.data_out,pixel)    // load new pixel when read_pixel is inserted
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
      when(!frame_capture.io.buffer_full){ // when empty, done reading
        tx_fms:=idle
      }
    }
  }

  // sccb interface
  sccb_interface.io.control_address:=addr_field
  sccb_interface.io.config_data:=data_field
  sccb_interface.io.start:=start
  io.SIOC:=sccb_interface.io.SIOC
  io.SIOD:=sccb_interface.io.SIOD
  // frame buffer
  frame_capture.io.read_frame<>read_pixel
  frame_capture.io.p_clk<>io.p_clk
  frame_capture.io.href<>io.href
  frame_capture.io.vsync<>io.vsync
  frame_capture.io.data_in<>io.data_in
  val frame_done=WireInit(false.B)
  frame_capture.io.frame_done<>frame_done
  frame_capture.io.capture:=capture
  frame_capture.io.read_frame:=read_pixel
  // transmitter
  transmitter.io.channel.valid:=tx_valid // default is false
  transmitter.io.channel.bits:=tx_data
}

// SCCB frequency: 200k
object camera_top_v extends App {
  chisel3.Driver.execute(Array[String](), () => new camera_top(50000000,100000,
    640,480,2,115200))
}