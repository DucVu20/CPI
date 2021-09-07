//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//class TestMemoryControlUnit(dut : memory_control_unit[SInt]) extends PeekPokeTester(dut) {
//  var tmp_data : BigInt = BigInt(0)
//
//  step(1)
//  poke(dut.io.control.addr, 0.U)
//  poke(dut.io.control.en, false.B)
//  poke(dut.io.control.we, false.B)
//  poke(dut.io.data_in, tmp_data)
//  step(1)
//  //==================== RUN TEST ===========================================
//  for (i <- 0 until PARA.NO_TEST) {
//    println(s"================Test Case ${i + 1}=================================")
//    poke(dut.io.control.en, false.B)
//    poke(dut.io.control.we, true.B)
//    for (loop_count <- 0 until 160) {
//      poke(dut.io.control.addr, loop_count.asUInt())
//      tmp_data = PARA.in_range(Random.nextInt(), 8).asSInt()
//      println("Input = " + tmp_data.toString())
//      poke(dut.io.data_in, tmp_data)
//      step(1)
//    }
//    poke(dut.io.control.we, false.B)
//    poke(dut.io.control.en, true.B)
//    for (loop_count <- 0 until 10) {
//      poke(dut.io.control.addr, (loop_count).asUInt())
//      println("Result = " + peek(dut.io.data_out).toString())
//      step(1)
//    }
//  }
//}
//
//
//object TestMemoryControlUnit extends App {
//  chisel3.iotesters.Driver(() => new memory_control_unit(1024, 16, SInt(8.W))) {
//    c => new TestMemoryControlUnit(c)
//  }
//}
//
//object TestMemoryControlUnitVerilog extends App {
//  (new ChiselStage).emitVerilog(new memory_control_unit(1024, 16, SInt(8.W)))
//}
//
//class WaveformMemoryControlUnit extends FlatSpec with Matchers {
//  "WaveformMemoryControlUnit" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new memory_control_unit(1024, 16, SInt(8.W))) {
//      c => new TestMemoryControlUnit(c)
//    } should be (true)
//  }
//}