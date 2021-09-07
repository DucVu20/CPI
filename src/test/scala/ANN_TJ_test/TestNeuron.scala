//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//class TestNeuron(dut: neuron) extends PeekPokeTester(dut) {
//  var tmp_w : Array[BigInt] = Array.fill(PARA.NUM_CONCURRENT)(BigInt(0))
//  var tmp_a : Array[BigInt] = Array.fill(PARA.NUM_CONCURRENT)(BigInt(0))
//  var tmp_b : SInt = 0.S
//  step(1)
//  poke(dut.io.clear, true.B)
//  poke(dut.io.start, false.B)
//  poke(dut.io.mac_start, false.B)
//  poke(dut.io.activate, false.B)
//  poke(dut.io.w, tmp_w)
//  poke(dut.io.a, tmp_a)
//  poke(dut.io.b, tmp_b)
//  step(1)
//  //==================== RUN TEST ===========================================
//  var tmp_sum : Array[BigInt] = Array.fill(PARA.NUM_CONCURRENT)(BigInt(0))
//  for (i <- 0 until PARA.NO_TEST) {
//    println(s"Test Case ${i + 1}")
//    tmp_b = PARA.in_range(Random.nextInt(), PARA.BIT_WIDTH_W).asSInt()
//    for (stt <- 0 until PARA.NUM_CONCURRENT) {
//      tmp_w(stt) = PARA.in_range(Random.nextInt(), PARA.BIT_WIDTH_W).asSInt()
//      tmp_a(stt) = PARA.in_range(Random.nextInt(), PARA.BIT_WIDTH_A).asSInt()
//      tmp_sum(stt) = (tmp_w(stt) * tmp_a(stt)).asSInt()
//    }
//    poke(dut.io.clear, false.B)
//    poke(dut.io.start, true.B)
//    poke(dut.io.mac_start, true.B)
//    poke(dut.io.activate, false.B)
//    poke(dut.io.w, tmp_w)
//    poke(dut.io.a, tmp_a)
//    poke(dut.io.b, tmp_b)
//    println("Pair Input = " + tmp_w.mkString("Array(", ", ", ")") + " and " + tmp_a.mkString("Array(", ", ", ")") + " with " + tmp_b.toInt)
//    step(1)
//    poke(dut.io.mac_start, false.B)
//    step(PARA.BIT_WIDTH_W)
//    poke(dut.io.activate, true.B)
//    step(1)
//    poke(dut.io.activate, false.B)
//    println("Result is: " + peek(dut.io.result))
//    //expect(dut.io.result, (0 until PARA.NUM_CONCURRENT).map(tmp_sum(_)).sum + tmp_b)
//    step(1)
//    poke(dut.io.clear, true.B)
//    step(1)
//    poke(dut.io.clear, false.B)
//  }
//  //=========================================================================
//}
//
//object TestNeuron extends App {
//  chisel3.iotesters.Driver(() => new neuron(
//    FRACTION_WIDTH = 2,
//    PARA.NUM_CONCURRENT,
//    PARA.NUM_BIT_IN,
//    PARA.BIT_WIDTH_W,
//    PARA.BIT_WIDTH_A,
//    PARA.NUM_EXT_BIT)) {
//    c => new TestNeuron(c)
//  }
//}
//
//object TestNeuronVerilog extends App {
//  (new ChiselStage).emitVerilog(new neuron(
//    FRACTION_WIDTH = 2,
//    PARA.NUM_CONCURRENT,
//    PARA.NUM_BIT_IN,
//    PARA.BIT_WIDTH_W,
//    PARA.BIT_WIDTH_A,
//    PARA.NUM_EXT_BIT))
//}
//
//class WaveformNeuron extends FlatSpec with Matchers {
//  "WaveformNeuron" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new neuron(
//      FRACTION_WIDTH = 2,
//      PARA.NUM_CONCURRENT,
//      PARA.NUM_BIT_IN,
//      PARA.BIT_WIDTH_W,
//      PARA.BIT_WIDTH_A,
//      PARA.NUM_EXT_BIT)) {
//      c => new TestNeuron(c)
//    } should be (true)
//  }
//}