package cpi

import chisel3._
import chisel3.util._

class SinglePortRam[T <: Data](mem_depth: Int,
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
