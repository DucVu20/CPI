package CPI

import chisel3._
import chisel3.util._

class DualClockRam[T <: Data](memDepth: Int,
                              gen: T) extends Module{
  val depth=memDepth

  val io=IO(new Bundle{
    val readAddr   = Input(UInt(log2Ceil(memDepth).W))
    val wrAddr     = Input(UInt(log2Ceil(memDepth).W))
    val dataIn     = Input(gen)
    val dataOut    = Output(gen)
    val wrEna      = Input(Bool())
    val writeClock = Input(Clock())
  })
  val buffer = SyncReadMem(memDepth, gen)

  withClock(io.writeClock){
    when(io.wrEna){
      buffer.write(io.wrAddr,io.dataIn)
    }
  }
  io.dataOut := buffer.read(io.readAddr)
}

class DualClockRamDemo[T <: Data](memDepth: Int,
                                  gen: T) extends Module{
  val io=IO(new Bundle{
    val readAddr   = Input(UInt(log2Ceil(memDepth).W))
    val wrAddr     = Input(UInt(log2Ceil(memDepth).W))
    val dataIn     = Input(gen)
    val dataOut    = Output(gen)
    val wrEna      = Input(Bool())
    val preScaler  = Input(UInt(log2Ceil(64).W))
    val pclk       = Output(Bool())
    // convert pclk to Bool type for the sake of testing
  })
  val dualClockRam   = Module(new DualClockRam[T](memDepth, gen))
  val clockGenerator = Module(new clockDivider(64))
  val pclk           = clockGenerator.io.divided_clock

  clockGenerator.io.divided_clock.asBool() <>io.pclk
  clockGenerator.io.reset     <> reset
  clockGenerator.io.clock_in  <> clock
  clockGenerator.io.prescaler <> io.preScaler
  dualClockRam.io.writeClock  <> clockGenerator.io.divided_clock

  dualClockRam.io.readAddr <> io.readAddr
  dualClockRam.io.wrAddr   <> io.wrAddr
  dualClockRam.io.dataIn   <> io.dataIn
  dualClockRam.io.dataOut  <> io.dataOut
  dualClockRam.io.wrEna    <> io.wrEna
}

