package sislab.cpi

import chisel3._
import chisel3.util._

class SinglePortRam[T <: Data](memDepth: Int,
                               gen: T) extends Module {
  val depth = memDepth
  val io = IO(new Bundle {
    val addr     = Input(UInt(log2Ceil(memDepth).W))
    val dataIn  = Input(gen)
    val dataOut = Output(gen)
    val wrEna    = Input(Bool())
    val rdEna    = Input(Bool())
  })
  val mem = SyncReadMem(memDepth, gen)
  when(io.wrEna) {
    mem.write(io.addr, io.dataIn)
  }
  io.dataOut := mem.read(io.addr, io.rdEna)
}