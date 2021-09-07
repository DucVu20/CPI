package sislab.cpi
import chisel3._
import chisel3.util._


class camera_module(width: Int, height: Int,
                    max_byte_per_pixel: Int) extends Module {
  val w                = width
  val h                = height
  val frame_resolution = width * height // the maximum depth of the buffer
  val pixel_bits       = 8 * max_byte_per_pixel

  val io  = IO(new Bundle {
    val p_clk          = Input (Bool())
    val href           = Input (Bool())
    val vsync          = Input (Bool())
    val pixel_in       = Input (UInt(8.W))
    val pixel_out      = Output(UInt(pixel_bits.W))
    val pixel_addr     = Output(UInt(log2Ceil(frame_resolution).W))
    val frame_width    = Output(UInt(log2Ceil(640).W))
    val frame_height   = Output(UInt(log2Ceil(480).W))
    val byte_per_pixel = Input (UInt(log2Ceil(max_byte_per_pixel).W))
    val capture        = Input (Bool())
    val capturing      = Output(Bool())
    val read_frame     = Input (Bool()) // ready
    val frame_full     = Output(Bool()) // valid

  })
  val idle :: capture_frame :: Nil = Enum(2)
  val FMS = RegInit(idle)

  val write_Ptr            = RegInit(0.U(log2Ceil(frame_resolution).W))
  val read_Ptr             = RegInit(0.U(log2Ceil(frame_resolution).W))
  val first_byte           = RegInit(0.U(8.W))
  val second_byte          = RegInit(0.U(8.W))
  val pixel_counter        = RegInit(0.U(max_byte_per_pixel.W))
  val capture_reg          = RegInit(false.B)
  val buffer_depth_counter = RegInit(0.U(log2Ceil(frame_resolution).W))
  val frame_done           = RegInit(false.B)
  val row_cnt              = RegInit(0.U(log2Ceil(480).W))
  val col_cnt              = RegInit(0.U(log2Ceil(640).W))

  val wrEna_wire = WireInit(false.B)
  val addr       = WireInit(0.U(log2Ceil(frame_resolution).W))
  val capturing  = WireInit(false.B)
  val wrEna      = RegNext(wrEna_wire)

  val buffer     = Module(new single_port_ram(frame_resolution,
    UInt((max_byte_per_pixel * 8).W)))

  val pclk_risingEdge    = (io.p_clk) & (!RegNext(io.p_clk))
  val vsync_fallingEdge  = (!io.vsync) & (RegNext(io.vsync))
  capture_reg           := Mux(io.capture, io.capture, capture_reg)

  //==============READ ADDRESS GENERATOR==================//
  when(io.read_frame) {
    addr     := read_Ptr
    read_Ptr := read_Ptr + 1.U
    when(read_Ptr === (buffer_depth_counter - 1.U)) {
      read_Ptr             := 0.U
      buffer_depth_counter := 0.U
      frame_done:=false.B
    }
  } otherwise {
    addr := RegNext(write_Ptr)
  }

  //====================FMS for capturing images============================//
  switch(FMS) {
    is(idle) {

      when(io.vsync) {
        FMS := idle
      }.otherwise {
        when(capture_reg) {
          when(vsync_fallingEdge) {
            FMS         := capture_frame
            capture_reg := false.B
            frame_done  := false.B
          }
        }
      }
      write_Ptr  := 0.U
      capturing  := false.B
    }
    is(capture_frame) {

      capturing  := true.B
      frame_done := Mux(io.vsync, true.B, false.B)
      FMS        := Mux(io.vsync, idle, capture_frame)

      when(pclk_risingEdge && (io.href)) {

        pixel_counter := pixel_counter + 1.U
        switch(pixel_counter) {
          is(0.U) {
            first_byte  := io.pixel_in
          }
          is(1.U) {
            second_byte := io.pixel_in
          }
        }
        when(pixel_counter === io.byte_per_pixel - 1.U) {
          pixel_counter := 0.U
          write_Ptr     := write_Ptr + 1.U // update pixel address
          wrEna_wire    := pclk_risingEdge
          col_cnt       := col_cnt + 1.U
          buffer_depth_counter := buffer_depth_counter + 1.U
        }
      }
    }
  }
  val pixel = Cat(second_byte, first_byte) // RGB, MSB: R, LSB: B
  buffer.io.wrEna   := wrEna
  buffer.io.rdEna   := io.read_frame
  buffer.io.addr    := addr
  buffer.io.data_in := pixel


  //============================IO=========================================//
  io.capturing     := capturing
  io.frame_width   := col_cnt
  io.frame_height  := row_cnt
  io.pixel_addr    := RegNext(read_Ptr)
  io.pixel_out     := buffer.io.data_out
  io.frame_full    := frame_done
  // io.row_interrupt := interrupt

  //==============SPECIFY RESOLUTION BASED ON HREF, VSYNC ==================//
  when((io.href) & (!RegNext(io.href))) {
    row_cnt := row_cnt + 1.U
    col_cnt := 0.U
  }
  when(vsync_fallingEdge) {
    row_cnt := 0.U
    col_cnt := 0.U
  }

}

class single_port_ram[T <: Data](mem_depth: Int,
                                 gen: T) extends Module {
  val addr_width = log2Ceil(mem_depth)
  val io = IO(new Bundle {
    val addr     = Input(UInt(addr_width.W))
    val data_in  = Input(gen)
    val data_out = Output(gen)
    val wrEna    = Input(Bool())
    val rdEna    = Input(Bool())
  })
  val mem = SyncReadMem(mem_depth, gen)
  when(io.wrEna) {
    mem.write(io.addr, io.data_in)
  }
  io.data_out := mem.read(io.addr, io.rdEna)
}
