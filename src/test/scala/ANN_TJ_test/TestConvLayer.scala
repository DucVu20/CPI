//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//class TestConvLayer(dut: conv_layer) extends PeekPokeTester(dut) {
//  var tmp_data   : BigInt = BigInt(0)
//  var tmp_weight : BigInt = BigInt(0)
//
//  var tmp_stride      : BigInt = BigInt(0)
//  var tmp_filter_size : BigInt = BigInt(0)
//  var tmp_matrix_size : BigInt = BigInt(0)
//  step(1)
//  poke(dut.io.start_in, false.B)
//  poke(dut.io.data_ena, false.B)
//  poke(dut.io.data_in, tmp_data)
//  poke(dut.io.weight_ena, false.B)
//  poke(dut.io.weight_in, tmp_weight)
//  poke(dut.io.stride_in, 0.U)
//  poke(dut.io.filter_size_in, 0.U)
//  poke(dut.io.matrix_size_in, 0.U)
//  step(1)
//  //==================== RUN TEST ===========================================
//  for (i <- 0 until PARA.NO_TEST) {
//    var loop_count = 0
//    var filter_count = 0
//    println(s"================Test Case ${i + 1}=================================")
//    poke(dut.io.start_in, true.B)
//
//    tmp_stride      = PARA.in_range_pos(Random.nextInt(), 2).asUInt()
//    tmp_filter_size = (PARA.in_range_pos(Random.nextInt(), 2) + 1).asUInt()
//    tmp_matrix_size = (PARA.in_range_pos(Random.nextInt(), 5) + 1).asUInt()
////    tmp_stride      = 1.U
////    tmp_filter_size = 2.U
////    tmp_matrix_size = 4.U
//
//    poke(dut.io.stride_in, tmp_stride)
//    poke(dut.io.filter_size_in, tmp_filter_size)
//    poke(dut.io.matrix_size_in, tmp_matrix_size)
//    step(1)
//    poke(dut.io.start_in, false.B)
//    // Loop until completing scanning all the matrix
//    while (loop_count <= (tmp_matrix_size - tmp_filter_size).toInt) {
//      // Loop until the end of the row of matrix
//      // for (x <- 0 until PARA.MATRIX_WIDTH * PARA.FILTER_HEIGHT) {
//      for (x <- 0 until (tmp_matrix_size + 1.U).toInt * (tmp_filter_size + 1.U).toInt) {
//        // Generate data
//        tmp_data = PARA.in_range(Random.nextInt(), PARA.DBIT_WIDTH).asSInt()
//        tmp_weight = PARA.in_range(Random.nextInt(), PARA.FBIT_WIDTH).asSInt()
//
//        // Assign value into inputs
//        poke(dut.io.data_ena, true.B)
//        poke(dut.io.data_in, tmp_data)
//        println("Input = " + tmp_data.toString())
//        if (filter_count < (tmp_filter_size + 1.U).toInt * (tmp_filter_size + 1.U).toInt) {
//          filter_count += 1
//          poke(dut.io.weight_ena, true.B)
//          poke(dut.io.weight_in, tmp_weight)
//          println("Filter = " + tmp_weight.toString())
//        } else {
//          poke(dut.io.weight_ena, false.B)
//          poke(dut.io.weight_in, BigInt(0))
//        }
//        step(1)
//        // Extract result
//        println("Result is: " + peek(dut.io.data_out))
//      }
//      // Move to the next row of matrix
//      loop_count += (tmp_stride + 1.U).toInt
//    }
//    // IDLE for the next test case
//    poke(dut.io.data_ena, false.B)
//    step(10)
//  }
//}
//
//object TestConvLayer extends App {
//  chisel3.iotesters.Driver(() => new conv_layer(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.FBIT_WIDTH,
//    PARA.DBIT_WIDTH)) {
//    c => new TestConvLayer(c)
//  }
//}
//
//object TestConvLayerVerilog extends App {
//  (new ChiselStage).emitVerilog(new conv_layer(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.FBIT_WIDTH,
//    PARA.DBIT_WIDTH))
//}
//
//object TestConvLayerSystemVerilog extends App {
//  (new ChiselStage).emitSystemVerilog(new conv_layer(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.FBIT_WIDTH,
//    PARA.DBIT_WIDTH))
//}
//
//class WaveformConvLayer extends FlatSpec with Matchers {
//  "WaveformConvLayer" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new conv_layer(
//      PARA.MATRIX_WIDTH,
//      PARA.FILTER_WIDTH,
//      NUM_EXT_BIT = 5,
//      PARA.FBIT_WIDTH,
//      PARA.DBIT_WIDTH)) {
//      c => new TestConvLayer(c)
//    } should be (true)
//  }
//}