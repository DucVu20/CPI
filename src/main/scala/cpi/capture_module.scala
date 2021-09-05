package sislab.cpi

import chisel3._
import chisel3.util._

/**
it's important to note that the camera only capture images when it receives a
 capturing signal.
 When signals of a frame already start, the camera will hold the capturing signal
 from user's interface to capture images when a new frame starts and it will reset
 the capturing control register back to false

 *Note that this design use a capture signal to capture images, thus users can
 acquire images at different fts because the "capture signal" can be adjusted in time.
 - if the maximum frames/s is 30fps, by using the "capture signal", you can get video
  with less than 30fps
 */

/**
The first version, ver0, needs to know the depth or resolution of the camera in advance in order to
 generate a full signal for the buffer. The advantage of this design is that it knows when a
 frame has been captured before the camera captures the next frame. Thus we could do computation
 on that frame while waiting for a a new frame to be captured.

 According to the datasheet, the gap between when the buffer is full and the beginning of the
 next frame is 30 t_line, equivalent to 47040 clock cycles ( of the camera). If the CNN runs at
 high frequency, this design may be beneficial. The benefit is reinforced when dual-port ram is
 deployed as it can write data to the buffer while the CNN is reading data from the buffer.

 */
class capture_module(img_width:Int, img_height: Int,
                                     byte_per_pixel: Int) extends Module{
  // img_width=col, img_height=row

  val width=img_width
  val height=img_height
  val frame_resolution=width*height   // the maximum depth of the buffer
  val pixel_bit=8*byte_per_pixel

  val io=IO(new Bundle{
    val p_clk=Input(Bool())
    val href=Input(Bool())
    val vsync=Input(Bool())
    val pixel_in=Input(UInt(8.W))
    val pixel_out=Output(UInt(pixel_bit.W))          // 16 for ov7670
    val pixel_addr=Output(UInt(log2Ceil(frame_resolution).W))
    val frame_depth_in=Input(UInt(log2Ceil(frame_resolution).W))    // the image depth or resolution (wxh) of a mode
    // This value needs to be known and set by software
    val frame_width=Output(UInt(log2Ceil(640).W))
    val frame_height=Output(UInt(log2Ceil(480).W))
    val capture=Input(Bool())
    val read_frame=Input(Bool())    // ready
    val buffer_full=Output(Bool())  // valid
    val capturing=Output(Bool())
  })
  val idle::capture_frame::Nil=Enum(2)

  val FMS=RegInit(idle)
  val depth_reg=RegInit(0.U(log2Ceil(frame_resolution).W))
  val write_Ptr= RegInit(0.U(log2Ceil(frame_resolution).W))
  val read_Ptr= RegInit(0.U(log2Ceil(frame_resolution).W))

  val full=RegInit(false.B)
  val pixel_addr=RegNext(read_Ptr)
  val pclk_risingEdge = (io.p_clk)& (!RegNext(io.p_clk))   // detect edge only when p_clock rises from low to high and href is true
  val vsync_fallingEdge=(!io.vsync)&(RegNext(io.vsync))       // when vsync goes low, a new frame start, writePtr should be reset to 0

  val first_byte=RegInit(0.U(8.W))
  val second_byte=RegInit(0.U(8.W))
  val pixel_val = Cat(second_byte,first_byte)     // RGB, MSB: R, LSB: B

  val pixel_counter=RegInit(0.U(byte_per_pixel.W))

  val wrEna_wire=WireInit(false.B)
  val wrEna=RegNext(wrEna_wire)

  val capture_reg=RegInit(false.B)
  capture_reg:=Mux(io.capture,io.capture,capture_reg)

  val buffer=Module(new single_port_ram(frame_resolution,UInt((byte_per_pixel*8).W)))

  val addr=WireInit(0.U(log2Ceil(frame_resolution).W))

  val read_frame=io.read_frame&&full  // only read buffer when it's full and there's a signal
  val capturing=WireInit(false.B)

  //============================IO=========================================//
  io.capturing:=capturing
  buffer.io.wrEna:=wrEna
  buffer.io.rdEna:=read_frame
  buffer.io.addr:=addr
  buffer.io.data_in:=pixel_val
  depth_reg:=io.frame_depth_in

  //==============SPECIFY RESOLUTION BASED ON HREF, VSYCN ==================//
  val row_cnt=RegInit(0.U(log2Ceil(480).W))
  val col_cnt=RegInit(0.U(log2Ceil(640).W))

  when((io.href)& (!RegNext(io.href))){
    row_cnt:=row_cnt+1.U
    col_cnt:=0.U
  }
  when(vsync_fallingEdge){
    row_cnt:=0.U
    col_cnt:=0.U
  }
  io.frame_width:=col_cnt
  io.frame_height:=row_cnt
  io.pixel_addr:=RegNext(read_Ptr)
  io.pixel_out:=buffer.io.data_out
  io.buffer_full:=full

  //==============READ ADDRESS GENERATOR==================//
  when(read_frame){
    addr:=read_Ptr
    read_Ptr:=read_Ptr+1.U
    when(read_Ptr===(depth_reg-1.U)){
      read_Ptr:=0.U
      full:=false.B
    }
  }otherwise{
    addr:=RegNext(write_Ptr)
  }

  //====================FMS for capturing images============================//
  switch(FMS){
    is(idle){
      when(io.vsync){
        FMS:=idle
      }.otherwise{
        when(capture_reg){
          when(vsync_fallingEdge){
            FMS:=capture_frame
            capture_reg:=false.B
            full:=false.B
          }
        }
      }
      write_Ptr:=0.U
      write_Ptr:=0.U
      capturing:=false.B
    }
    is(capture_frame){
      capturing:=true.B
      FMS:=Mux(io.vsync,idle,capture_frame)
      when(pclk_risingEdge&&(io.href)){
        pixel_counter:=pixel_counter+1.U
        switch(pixel_counter){
          is(0.U) {first_byte:=io.pixel_in}
          is(1.U) {second_byte:=io.pixel_in}
        }
        when(pixel_counter===(byte_per_pixel-1).U){
          pixel_counter:=0.U
          write_Ptr:=write_Ptr+1.U              // update pixel address
          wrEna_wire:=pclk_risingEdge
          col_cnt:=col_cnt+1.U
        }
      }
      when(write_Ptr===(depth_reg-1.U)){
        full:=true.B
      }
    }
  }
}
class single_port_ram[T<:Data](mem_depth: Int,
                               gen: T) extends Module{
  val addr_width=log2Ceil(mem_depth)
  val io=IO(new Bundle{
    val addr=Input(UInt(addr_width.W))
    val data_in=Input(gen)
    val data_out=Output(gen)
    val wrEna=Input(Bool())
    val rdEna=Input(Bool())
  })
  val mem=SyncReadMem(mem_depth,gen)
  when(io.wrEna){
    mem.write(io.addr,io.data_in)
  }
  io.data_out:=mem.read(io.addr,io.rdEna)
}
