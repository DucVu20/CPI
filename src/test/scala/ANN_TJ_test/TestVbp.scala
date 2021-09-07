//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//class TestVbp(dut: vbp) extends PeekPokeTester(dut) {
//  // Testcase 0
//  step(1)
//  poke(dut.io.start_in, true.B)
//  var tmp_w : Array[BigInt] = Array.fill(PARA.NUM_CONCURRENT)(BigInt(0))
//  poke(dut.io.w, tmp_w)
//  var tmp_a : Array[BigInt] = Array.fill(PARA.NUM_CONCURRENT)(BigInt(0))
//  poke(dut.io.a, tmp_a)
//  println("Pair Input = " + tmp_w.mkString("Array(", ", ", ")") + " and " + tmp_a.mkString("Array(", ", ", ")"))
//  step(1)
//  poke(dut.io.start_in, false.B)
//  step(PARA.BIT_WIDTH_W-1)
//  println("Result is: " + peek(dut.io.sum))
//  step(1)
//  //=========== RUN TEST ================================================
//  var tmp_sum : Array[BigInt] = Array.fill(PARA.NUM_CONCURRENT)(BigInt(0))
//  for (i <- 0 until PARA.NO_TEST) {
//    println(s"Test Case ${i+1}")
//    for (stt <- 0 until PARA.NUM_CONCURRENT) {
//      tmp_w(stt)   = PARA.in_range(Random.nextInt(), PARA.BIT_WIDTH_W).asSInt()
//      tmp_a(stt)   = PARA.in_range(Random.nextInt(), PARA.BIT_WIDTH_A).asSInt()
//      tmp_sum(stt) = (tmp_w(stt) * tmp_a(stt)).asSInt()
//    }
//    poke(dut.io.start_in, true.B)
//    poke(dut.io.w, tmp_w)
//    poke(dut.io.a, tmp_a)
//    println("Pair Input = " + tmp_w.mkString("Array(", ", ", ")") + " and " + tmp_a.mkString("Array(", ", ", ")"))
//    step(1)
//    poke(dut.io.start_in, false.B)
//    step(PARA.BIT_WIDTH_W-2)
//    println("Result is: " + peek(dut.io.sum))
//    expect(dut.io.sum, (0 until PARA.NUM_CONCURRENT).map(tmp_sum(_)).sum)
//    step(1)
//  }
//  //======================================================================
//}
//
//object TestVbp extends App {
//  chisel3.iotesters.Driver(() => new vbp(
//    PARA.NUM_BIT_IN,
//    PARA.BIT_WIDTH_W,
//    PARA.BIT_WIDTH_A,
//    PARA.NUM_CONCURRENT,
//    PARA.NUM_EXT_BIT)) {
//    c => new TestVbp(c)
//  }
//}
//
//object TestVbpVerilog extends App {
//  (new ChiselStage).emitVerilog(new vbp(
//    PARA.NUM_BIT_IN,
//    PARA.BIT_WIDTH_W,
//    PARA.BIT_WIDTH_A,
//    PARA.NUM_CONCURRENT,
//    PARA.NUM_EXT_BIT))
//}
//
//class WaveformVBP extends FlatSpec with Matchers {
//  "WaveformVBP" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new vbp(
//      PARA.NUM_BIT_IN,
//      PARA.BIT_WIDTH_W,
//      PARA.BIT_WIDTH_A,
//      PARA.NUM_CONCURRENT,
//      PARA.NUM_EXT_BIT)) {
//      c => new TestVbp(c)
//    } should be (true)
//  }
//}