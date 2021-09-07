//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//class TestPoolLayer(dut: pool_layer) extends PeekPokeTester(dut) {
//  var tmp_data : BigInt = BigInt(0)
//
//  var tmp_stride      : BigInt = BigInt(0)
//  var tmp_filter_size : BigInt = BigInt(0)
//  var tmp_matrix_size : BigInt = BigInt(0)
//  step(1)
//  poke(dut.io.start_in, false.B)
//  poke(dut.io.data_ena, false.B)
//  poke(dut.io.data_in, tmp_data)
//  poke(dut.io.stride_in, tmp_stride)
//  poke(dut.io.filter_size_in, tmp_filter_size)
//  poke(dut.io.matrix_size_in, tmp_matrix_size)
//  step(1)
//  //==================== RUN TEST ===========================================
//  for (i <- 0 until PARA.NO_TEST) {
//    var loop_count   = 0
//    var data_count   = 0
//    var stride_count = 0
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
//    step(20)
//    var tmp_result : Array[BigInt] = Array.fill(tmp_filter_size.toInt)(BigInt(0))
//    // Loop until completing scanning all the matrix
//    while (loop_count <= (tmp_matrix_size - tmp_filter_size).toInt) {
//      // Loop until the end of the row of matrix
//      // for (x <- 0 until PARA.MATRIX_WIDTH * PARA.FILTER_HEIGHT) {
//      for (x <- 0 until (tmp_matrix_size + 1.U).toInt * (tmp_filter_size + 1.U).toInt) {
//        // Generate data
//        tmp_data = PARA.in_range_pos(Random.nextInt(), PARA.DBIT_WIDTH).asUInt()
//
//        // Assign value into inputs
//        poke(dut.io.data_ena, true.B)
//        poke(dut.io.data_in, tmp_data)
//        println("Input = " + tmp_data.toString())
//        tmp_result(data_count) = tmp_data
//        if (data_count == (tmp_filter_size - 1).toInt) {
//          data_count = 0
//        } else {
//          data_count += 1
//        }
//        step(1)
//        if (x >= (x % tmp_filter_size.toInt)) {
//          if (stride_count == 0) {
//            // Extract result
//            println("Result is: " + peek(dut.io.data_out))
//            expect(dut.io.data_out, tmp_result.max)
//          }
//
//          if (stride_count == tmp_stride.toInt) {
//            stride_count = 0
//          } else {
//            stride_count += 1
//          }
//        }
//        poke(dut.io.data_ena, false.B)
//        step(2)
//      }
//      // Move to the next row of matrix
//      loop_count += (tmp_stride + 1.U).toInt
//    }
//    // IDLE for the next test case
//    step(10)
//  }
//}
//
//object TestPoolLayer extends App {
//  chisel3.iotesters.Driver(() => new pool_layer(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    PARA.DBIT_WIDTH)) {
//    c => new TestPoolLayer(c)
//  }
//}
//
//object TestPoolLayerVerilog extends App {
//  (new ChiselStage).emitVerilog(new pool_layer(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    PARA.DBIT_WIDTH))
//}
//
//class WaveformPoolLayer extends FlatSpec with Matchers {
//  "WaveformPoolLayer" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new pool_layer(
//      PARA.MATRIX_WIDTH,
//      PARA.FILTER_WIDTH,
//      PARA.DBIT_WIDTH)) {
//      c => new TestPoolLayer(c)
//    } should be (true)
//  }
//}
