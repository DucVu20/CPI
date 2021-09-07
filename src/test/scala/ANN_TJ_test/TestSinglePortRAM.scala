//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//class TestSinglePortSIntRAM(dut: single_port_ram[SInt]) extends PeekPokeTester(dut) {
//  var tmp_data : BigInt = BigInt(0)
//
//  step(1)
//  poke(dut.io.control.addr, 0.U)
//  poke(dut.io.data_in, tmp_data)
//  poke(dut.io.control.en, false.B)
//  poke(dut.io.control.we, false.B)
//  step(1)
//  //==================== RUN TEST ===========================================
//  for (i <- 0 until PARA.NO_TEST) {
//    println(s"================Test Case ${i + 1}=================================")
//    poke(dut.io.control.en, true.B)
//    step(1)
//    poke(dut.io.control.en, false.B)
//    poke(dut.io.control.we, true.B)
//    for (loop_count <- 0 until 100) {
//      poke(dut.io.control.addr, loop_count.asUInt())
//      tmp_data = PARA.in_range(Random.nextInt(), 8).asSInt()
//      println("Input = " + tmp_data.toString())
//      poke(dut.io.data_in, tmp_data)
//      step(1)
//    }
//    poke(dut.io.control.we, false.B)
//    poke(dut.io.control.en, true.B)
//    for (loop_count <- 0 until 100) {
//      poke(dut.io.control.addr, loop_count.asUInt())
//      println("Result = " + peek(dut.io.data_out).toString())
//      step(1)
//    }
//  }
//}
//class TestSinglePortUIntRAM(dut: single_port_ram[UInt]) extends PeekPokeTester(dut) {
//  var tmp_data : BigInt = BigInt(0)
//
//  step(1)
//  poke(dut.io.control.addr, 0.U)
//  poke(dut.io.data_in, tmp_data)
//  poke(dut.io.control.en, false.B)
//  poke(dut.io.control.we, false.B)
//  step(1)
//  //==================== RUN TEST ===========================================
//  for (i <- 0 until PARA.NO_TEST) {
//    println(s"================Test Case ${i + 1}=================================")
//    poke(dut.io.control.en, true.B)
//    step(1)
//    poke(dut.io.control.en, false.B)
//    poke(dut.io.control.we, true.B)
//    for (loop_count <- 0 until 100) {
//      poke(dut.io.control.addr, loop_count.asUInt())
//      tmp_data = PARA.in_range_pos(Random.nextInt(), 8).asUInt()
//      println("Input = " + tmp_data.toString())
//      poke(dut.io.data_in, tmp_data)
//      step(1)
//    }
//    poke(dut.io.control.we, false.B)
//    poke(dut.io.control.en, true.B)
//    for (loop_count <- 0 until 100) {
//      poke(dut.io.control.addr, loop_count.asUInt())
//      println("Result = " + peek(dut.io.data_out).toString())
//      step(1)
//    }
//  }
//}
//
//object TestSinglePortSIntRAM extends App {
//  chisel3.iotesters.Driver(() => new single_port_ram(1024, SInt(8.W))) {
//    c => new TestSinglePortSIntRAM(c)
//  }
//}
//
//object TestSinglePortUIntRAM extends App {
//  chisel3.iotesters.Driver(() => new single_port_ram(1024, UInt(8.W))) {
//    c => new TestSinglePortUIntRAM(c)
//  }
//}
//
//object TestSinglePortRAMVerilog extends App {
//  (new ChiselStage).emitVerilog(new single_port_ram(1024, SInt(8.W)))
//}
//
//class WaveformSinglePortRAM extends FlatSpec with Matchers {
//  "WaveformSinglePortRAM" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new single_port_ram(1024, SInt(8.W))) {
//      c => new TestSinglePortSIntRAM(c)
//    } should be (true)
//  }
//}