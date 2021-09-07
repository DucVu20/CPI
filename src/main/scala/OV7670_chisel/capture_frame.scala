package OV7670_chisel
import chisel3._
import chisel3.util._
import org.scalacheck.Prop.False
import scala.math.{ceil, log}

//class capture_frame(img_width:Int, img_height: Int, byte_per_pixel: Int, data_width: Int) extends Module{
//  // depth of the buffer= resolution*byte_per_pixel  //
//  val width=img_width
//  val height=img_height
//  val depth=img_width*img_height*byte_per_pixel
//  val io=IO(new Bundle{
//    val p_clk=Input(Bool())
//    val href=Input(Bool())
//    val vsync=Input(Bool())
//    val data_in=Input(UInt(8.W))
//    val frame_done=Output(Bool())
//    val data_out=Output(UInt(8.W))
//    val dout_addr=Output(UInt(log2Ceil(depth).W))
//    val dout_valid=Output(Bool())
//    val done_reading=Output(Bool())
//
//  })
//  val idle::capture_frame::Nil=Enum(2)
//  val FMS=RegInit(idle)
//  val depth_reg=RegInit(0.U(log2Ceil(depth).W))
//  val write_Ptr= RegInit(0.U(log2Ceil(depth).W))
//  val read_Ptr= RegInit(0.U(log2Ceil(depth).W))
//  val buffer = Module(new frame_buffer(depth, data_width ))
//  val pixel_valid=io.href&&(!io.p_clk)
//  val done_reading=Wire(Bool())
//  val read_buffer=RegInit(false.B)
//  val full=RegInit(false.B)
//  val dout_valid=RegNext(read_buffer)       // delay the output of this signal by 1 clock
//  val dout_addr=RegNext(read_Ptr)           // delay the output address by 1 clock so that dout is the value of that output addr register
//  val bufferfull_edge_detector=(full)&(!RegNext(full))
//  val pclk_risingEdge = (io.p_clk)& (!RegNext(io.p_clk))   // detect edge only when p_clock rises from low to high and href is true
//  val vsync_fallingEdge=(!io.vsync)&(RegNext(io.vsync))       // when vsync goes low, a new frame start, writePtr should be reset to 0
//  read_buffer:=Mux(bufferfull_edge_detector,bufferfull_edge_detector,read_buffer)             // keep this reg high for reading, when done, reset back to 0
//
//  depth_reg:=depth.asUInt()
//  done_reading:=false.B
//  buffer.io.wrAddr:=write_Ptr
//  buffer.io.wrData:=io.data_in
//  io.frame_done:=false.B
//  buffer.io.wrEna:=false.B
//
//  //========================FMS_with_datapath==================//
//  switch(FMS){
//    is(idle){
//      FMS:=Mux(io.vsync,idle,capture_frame)
//      write_Ptr:=0.U
//      io.frame_done:=false.B
//      write_Ptr:=0.U
//    }
//    is(capture_frame){
//      FMS:=Mux(io.vsync,idle,capture_frame)
//      when(pclk_risingEdge&&(io.href)){
//        write_Ptr:=write_Ptr+1.U
//        io.frame_done:=Mux(io.vsync,true.B,false.B)
//        buffer.io.wrEna:=pclk_risingEdge
//      }
//      when(write_Ptr===(depth_reg-1.U)){
//        full:=true.B          // buffer is full
//      }
//    }
//  }
//  //========================Read Port=========================//
//  // an additional register is added to delayed the output address by 1 clock cycle, so that the output address has
//  // is aligned with the address
//  when(read_buffer){
//    read_Ptr:=read_Ptr+1.U
//    when(read_Ptr===(depth_reg-1.U)){
//      read_Ptr:=0.U
//      done_reading:=true.B
//      read_buffer:=false.B            // shut so that read_ptr won't count even when full is still high
//    }
//  }
//  buffer.io.rdAddr:=read_Ptr
//
//  io.data_out:=buffer.io.rdData
//  io.dout_addr:=dout_addr
//  io.dout_valid:=dout_valid
//  io.done_reading:=done_reading
//}


class capture_frame_pixel_ver(img_width:Int, img_height: Int, byte_per_pixel: Int) extends Module{
  // depth of the buffer= resolution*byte_per_pixel  //
  val width=img_width
  val height=img_height
  val frame_resolution=width*height
  val pixel_bit=8*byte_per_pixel
  val io=IO(new Bundle{
    val p_clk=Input(Bool())
    val href=Input(Bool())
    val vsync=Input(Bool())
    val data_in=Input(UInt(8.W))
    val frame_done=Output(Bool())
    val data_out=Output(UInt(pixel_bit.W))          // 16 for ov7670
    val dout_addr=Output(UInt(log2Ceil(frame_resolution).W))
    val buffer_full=Output(Bool())
    val read_frame=Input(Bool())
    val capture=Input(Bool())
  })
  val idle::capture_frame::Nil=Enum(2)
  val FMS=RegInit(idle)
  val depth_reg=RegInit(0.U(log2Ceil(frame_resolution).W))
  val write_Ptr= RegInit(0.U(log2Ceil(frame_resolution).W))
  val read_Ptr= RegInit(0.U(log2Ceil(frame_resolution).W))
  val buffer = Module(new frame_buffer(frame_resolution, byte_per_pixel*8 ))
  val pixel_valid=io.href&&(!io.p_clk)
  val full=RegInit(false.B)
  val dout_addr=RegNext(read_Ptr)           // delay the output address by 1 clock so that dout is the value of that output addr register
  val pclk_risingEdge = (io.p_clk)& (!RegNext(io.p_clk))   // detect edge only when p_clock rises from low to high and href is true
  val vsync_fallingEdge=(!io.vsync)&(RegNext(io.vsync))       // when vsync goes low, a new frame start, writePtr should be reset to 0
  val read_frame=RegInit(false.B)
  read_frame:=Mux(io.read_frame,io.read_frame,read_frame)

  depth_reg:=frame_resolution.U
  val first_byte=RegInit(0.U(8.W))
  val second_byte=RegInit(0.U(8.W))
  val pixel_val = Cat(first_byte,second_byte)     // RGB, MSB: R, LSB: B
  val pixel_counter=RegInit(0.U(byte_per_pixel.W))
  val wrEna_wire=WireInit(false.B)
  val wrEna=RegNext(wrEna_wire)
  val capture_reg=RegInit(false.B)
  capture_reg:=Mux(io.capture,io.capture,capture_reg)

  buffer.io.wrEna:=wrEna
  buffer.io.wrAddr:=RegNext(write_Ptr)
  buffer.io.wrData:=pixel_val
  io.frame_done:=false.B

  //========================FMS with data path==================//
  switch(FMS){
    is(idle){
      when(io.vsync){
        FMS:=idle
      }.otherwise{
        when(capture_reg){
          when(vsync_fallingEdge){
            FMS:=capture_frame
            capture_reg:=false.B
          }
        }
      }
      write_Ptr:=0.U
      io.frame_done:=false.B
      write_Ptr:=0.U
    }
    is(capture_frame){
      FMS:=Mux(io.vsync,idle,capture_frame)
      when(pclk_risingEdge&&(io.href)){
        pixel_counter:=pixel_counter+1.U
        switch(pixel_counter){
          is(0.U) {first_byte:=io.data_in}
          is(1.U) {second_byte:=io.data_in}
        }
        when(pixel_counter===(byte_per_pixel-1).U){
          pixel_counter:=0.U
          write_Ptr:=write_Ptr+1.U              // update pixel address
          wrEna_wire:=pclk_risingEdge
        }
        io.frame_done:=Mux(io.vsync,true.B,false.B)
      }
      when(write_Ptr===(depth_reg-1.U)){
        full:=true.B          // buffer is full
      }
    }
  }
  //========================Read Port=========================//
  // an additional register is added to delayed the output address by 1 clock cycle, so that the output address has
  // is aligned with the address
  when(io.read_frame){
    read_Ptr:=read_Ptr+1.U
    when(read_Ptr===(depth_reg-1.U)){
      read_Ptr:=0.U
      full:=false.B
      //read_frame:=false.B
    }
  }
  buffer.io.rdAddr:=read_Ptr
  io.data_out:=buffer.io.rdData
  io.dout_addr:=dout_addr
  io.buffer_full:=full
}

class frame_buffer(depth:Int, data_width: Int) extends Module{
  val io=IO(new Bundle{
    val wrEna=Input(Bool())
    val wrData=Input(UInt(data_width.W))
    val wrAddr=Input(UInt(log2Ceil(depth).W))
    val rdAddr=Input(UInt(log2Ceil(depth).W))
    val rdData=Output(UInt(data_width.W))
  })
  val buffer = SyncReadMem( depth, UInt(data_width.W))
  when(io.wrEna) {
    buffer.write(io.wrAddr, io.wrData)
  }
  io.rdData := buffer.read(io.rdAddr)
}
object capture_frame_pixel_ver extends App {
  chisel3.Driver.execute(Array[String](), () => new capture_frame_pixel_ver(640,480,2))
}