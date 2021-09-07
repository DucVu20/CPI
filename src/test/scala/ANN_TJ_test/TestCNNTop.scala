//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//import java.io._
//
//class TestCNNTop(dut: cnn_top) extends PeekPokeTester(dut) {
//  var tmp_data   : BigInt = BigInt(0)
//  var tmp_weight : BigInt = BigInt(0)
//  var tmp_bias   : BigInt = BigInt(0)
//
//  var tmp_conv_stride      : BigInt = BigInt(0)
//  var tmp_conv_filter_size : BigInt = BigInt(0)
//  var tmp_conv_matrix_size : BigInt = BigInt(0)
//  var tmp_fc_no_layer      : BigInt = BigInt(0)
//  var tmp_conv_no_layer    : BigInt = BigInt(0)
//
//  var tmp_pool_stride      : BigInt = BigInt(0)
//  var tmp_pool_filter_size : BigInt = BigInt(0)
//  var tmp_pool_matrix_size : BigInt = BigInt(0)
//
//  step(1)
//  poke(dut.io.start_in, false.B)
//  poke(dut.io.data_ena, false.B)
//  poke(dut.io.data_in, tmp_data)
//  poke(dut.io.weight_ena, false.B)
//  poke(dut.io.weight_in, tmp_weight)
//
//  poke(dut.io.conv_stride_in, 0.U)
//  poke(dut.io.conv_filter_size_in, 0.U)
//  poke(dut.io.conv_matrix_size_in, 0.U)
//  poke(dut.io.pool_stride_in, 0.U)
//  poke(dut.io.pool_filter_size_in, 0.U)
//  poke(dut.io.pool_matrix_size_in, 0.U)
//  step(1)
//  //==================== RUN TEST ===========================================
//  for (i <- 0 until PARA.NO_TEST) {
//    var loop_count = 0
//    var filter_count = 0
//    println(s"================Test Case ${i + 1}=================================")
//    val file = new File(s"testResult$i.txt")
//    val pw = new BufferedWriter(new FileWriter(file))
//
//    poke(dut.io.start_in, true.B)
//    tmp_conv_stride      = PARA.in_range_pos(Random.nextInt(), 1).asUInt()
//    tmp_conv_filter_size = (PARA.in_range_pos(Random.nextInt(), 2) + 1).asUInt()
//    tmp_conv_matrix_size = (PARA.in_range_pos(Random.nextInt(), 4) + 12).asUInt()
//    poke(dut.io.conv_stride_in, tmp_conv_stride)
//    poke(dut.io.conv_filter_size_in, tmp_conv_filter_size)
//    poke(dut.io.conv_matrix_size_in, tmp_conv_matrix_size)
//    tmp_pool_stride      = PARA.in_range_pos(Random.nextInt(), 1).asUInt()
//    tmp_pool_filter_size = (PARA.in_range_pos(Random.nextInt(), 1) + 1).asUInt()
//    //tmp_pool_matrix_size = round((tmp_conv_matrix_size - tmp_conv_filter_size + 1.U).toInt / (tmp_conv_stride + 1.U).toFloat).asUInt()
//    tmp_pool_matrix_size = floor((tmp_conv_matrix_size - tmp_conv_filter_size).toInt / (tmp_conv_stride + 1.U).toInt).toInt.asUInt()
//    poke(dut.io.pool_stride_in, tmp_pool_stride)
//    poke(dut.io.pool_filter_size_in, tmp_pool_filter_size)
//    poke(dut.io.pool_matrix_size_in, tmp_pool_matrix_size)
//
//    step(1)
//    poke(dut.io.start_in, false.B)
//    // Loop until completing scanning all the matrix
//     while (loop_count <= (tmp_conv_matrix_size - tmp_conv_filter_size).toInt) {
//       // Loop until the end of the row of matrix
//       // for (x <- 0 until PARA.MATRIX_WIDTH * PARA.FILTER_HEIGHT) {
//       for (x <- 0 until (tmp_conv_matrix_size + 1.U).toInt * (tmp_conv_filter_size + 1.U).toInt) {
//         // Assign value into inputs
//         tmp_data = PARA.in_range_pos(Random.nextInt(), PARA.DBIT_WIDTH).asUInt()
//         tmp_weight = PARA.in_range(Random.nextInt(), PARA.FBIT_WIDTH).asSInt()
//         poke(dut.io.data_ena, true.B)
//         poke(dut.io.data_in, tmp_data)
//         println("Input = " + tmp_data.toString())
//         if (filter_count < (tmp_conv_filter_size + 1.U).toInt * (tmp_conv_filter_size + 1.U).toInt) {
//           filter_count += 1
//           poke(dut.io.weight_ena, true.B)
//           poke(dut.io.weight_in, tmp_weight)
//           println("Filter = " + tmp_weight.toString())
//         } else {
//           poke(dut.io.weight_ena, false.B)
//           poke(dut.io.weight_in, BigInt(0))
//         }
//         step(1)
//    // Extract result
//         if (peek(dut.io.result_done) == 1) {
//           println("Result = " + peek(dut.io.result_out).toString())
//           pw.write(peek(dut.io.result_out).toString())
//           pw.write("\n")
//         }
//       }
//       // Move to the next row of matrix
//       loop_count += (tmp_conv_stride + 1.U).toInt
//     }
//    // IDLE for the next test case
//    poke(dut.io.data_ena, false.B)
//    for (loop_count <- 0 until 20) {
//      println("Result = " + peek(dut.io.result_done))
//      if (peek(dut.io.result_done) == 1) {
//        println("Result = " + peek(dut.io.result_out).toString())
//        pw.write(peek(dut.io.result_out).toString())
//        pw.write("\n")
//      }
//      step(1)
//    }
//    pw.close()
//    step(20)
//  }
//}
//
//object TestCNNTop extends App {
//  chisel3.iotesters.Driver(() => new cnn_top(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.DBIT_WIDTH,
//    PARA.FBIT_WIDTH)) {
//    c => new TestCNNTop(c)
//  }
//}
//
//object TestCNNTopVerilog extends App {
//  (new ChiselStage).emitVerilog(new cnn_top(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.DBIT_WIDTH,
//    PARA.FBIT_WIDTH))
//}
//
//object TestCNNTopSystemVerilog extends App {
//  (new ChiselStage).emitSystemVerilog(new cnn_top(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.DBIT_WIDTH,
//    PARA.FBIT_WIDTH))
//}
//
//class WaveformCNNTop extends FlatSpec with Matchers {
//  "WaveformCNNTop" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new cnn_top(
//      PARA.MATRIX_WIDTH,
//      PARA.FILTER_WIDTH,
//      NUM_EXT_BIT = 5,
//      PARA.DBIT_WIDTH,
//      PARA.FBIT_WIDTH)) {
//      c => new TestCNNTop(c)
//    } should be (true)
//  }
//}