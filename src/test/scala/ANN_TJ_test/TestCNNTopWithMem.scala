//import chisel3._
//import chisel3.iotesters.Driver
//import chisel3.iotesters.PeekPokeTester
//import chisel3.stage.ChiselStage
//import org.scalatest._
//import scala.math._
//import scala.util.Random
//
//import scala.io.Source
//import java.io._
//
//
//class TestCNNTopWithMem(dut: cnn_top_with_mem) extends PeekPokeTester(dut) {
//  var tmp_data   : BigInt = BigInt(0)
//  var tmp_weight : BigInt = BigInt(0)
//  var tmp_bias   : BigInt = BigInt(0)
//
//  step(1)
//  poke(dut.io.start_in, false.B)
//  poke(dut.io.data_ena, false.B)
//  poke(dut.io.data_in, tmp_data)
//  poke(dut.io.weight_ena, false.B)
//  poke(dut.io.weight_in, tmp_weight)
//  poke(dut.io.bias_ena, false.B)
//  poke(dut.io.bias_in, tmp_bias)
//
//  poke(dut.io.conv_para_in.stride, 0.U)
//  poke(dut.io.conv_para_in.filter_size, 0.U)
//  poke(dut.io.conv_para_in.matrix_size, 0.U)
//  poke(dut.io.conv_para_in.layer_size, 0.U)
//  poke(dut.io.conv_para_in.dim_in, 0.U)
//  poke(dut.io.conv_para_in.dim_out, 0.U)
//
//  poke(dut.io.pool_para_in.stride, 0.U)
//  poke(dut.io.pool_para_in.filter_size, 0.U)
//  poke(dut.io.pool_para_in.matrix_size, 0.U)
//  poke(dut.io.pool_para_in.layer_size, 0.U)
//  poke(dut.io.pool_para_in.dim_in, 0.U)
//  poke(dut.io.pool_para_in.dim_out, 0.U)
//
//  poke(dut.io.fc_para_in.stride, 0.U)
//  poke(dut.io.fc_para_in.filter_size, 0.U)
//  poke(dut.io.fc_para_in.matrix_size, 0.U)
//  poke(dut.io.fc_para_in.layer_size, 0.U)
//  poke(dut.io.fc_para_in.dim_in, 0.U)
//  poke(dut.io.fc_para_in.dim_out, 0.U)
//  step(1)
//  //==================== RUN TEST ===========================================
//  for (i <- 0 until PARA.NO_TEST) {
//    println(s"================Test Case ${i + 1}=================================")
//    //======================== LOAD DATA =====================================//
//    val size_data = Source.fromFile(PARA.DIR_DATA_FILE + "data.txt").getLines.size
////    val data : Array[UInt] = Array.fill(size_data)(0.U(PARA.DBIT_WIDTH.W))
//    val data : Array[SInt] = Array.fill(size_data)(0.S(PARA.DBIT_WIDTH.W))
//    var count_data = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "data.txt").getLines) {
////      data(count_data) = line.toDouble.toInt.asUInt()
//      data(count_data) = line.toDouble.toInt.asSInt()
//      count_data = count_data + 1
//    }
//    //    println(s"THIS IS DATA!~")
//    //    println(data.mkString("Array:",",\n",""))
//    //======================== LOAD WEIGHT CONV1 =============================//
//    val size_w_conv1 = Source.fromFile(PARA.DIR_DATA_FILE + "conv1_w.txt").getLines.size
//    val w_conv1 : Array[SInt] = Array.fill(size_w_conv1)(0.S(PARA.FBIT_WIDTH.W))
//    var count_w1 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "conv1_w.txt").getLines) {
//      w_conv1(count_w1) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_w1 = count_w1 + 1
//    }
//    //    println(s"THIS IS WEIGHT OF CONVOLUTION LAYER 1!~")
//    //    println(w_conv1.mkString("Array(",",\n",")"))
//    //======================== LOAD BIAS CONV1 ===============================//
//    val size_b_conv1 = Source.fromFile(PARA.DIR_DATA_FILE + "conv1_b.txt").getLines.size
//    val b_conv1 : Array[SInt] = Array.fill(size_b_conv1)(0.S(PARA.FBIT_WIDTH.W))
//    var count_b1 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "conv1_b.txt").getLines) {
//      b_conv1(count_b1) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_b1 = count_b1 + 1
//    }
//    //    println(s"THIS IS BIAS OF CONVOLUTION LAYER 1!~")
//    //    println(b_conv1.mkString("Array(",",\n",")"))
//    //======================== LOAD WEIGHT CONV2 =============================//
//    val size_w_conv2 = Source.fromFile(PARA.DIR_DATA_FILE + "conv2_w.txt").getLines.size
//    val w_conv2 : Array[SInt] = Array.fill(size_w_conv2)(0.S(PARA.FBIT_WIDTH.W))
//    var count_w2 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "conv2_w.txt").getLines) {
//      w_conv2(count_w2) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_w2 = count_w2 + 1
//    }
//    //    println(s"THIS IS WEIGHT OF CONVOLUTION LAYER 2!~")
//    //    println(w_conv2.mkString("Array(",",\n",")"))
//    //======================== LOAD BIAS CONV2 ===============================//
//    val size_b_conv2 = Source.fromFile(PARA.DIR_DATA_FILE + "conv2_b.txt").getLines.size
//    val b_conv2 : Array[SInt] = Array.fill(size_b_conv2)(0.S(PARA.FBIT_WIDTH.W))
//    var count_b2 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "conv2_b.txt").getLines) {
//      b_conv2(count_b2) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_b2 = count_b2 + 1
//    }
//    //    println(s"THIS IS BIAS OF CONVOLUTION LAYER 2!~")
//    //    println(b_conv2.mkString("Array(",",\n",")"))
//    //======================== LOAD WEIGHT FC1 =============================//
//    val size_w_fc1 = Source.fromFile(PARA.DIR_DATA_FILE + "fc1_w.txt").getLines.size
//    val w_fc1 : Array[SInt] = Array.fill(size_w_fc1)(0.S(PARA.FBIT_WIDTH.W))
//    var count_fc1 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "fc1_w.txt").getLines) {
//      w_fc1(count_fc1) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_fc1 = count_fc1 + 1
//    }
//    //======================== LOAD BIAS FC1 ===============================//
//    val size_b_fc1 = Source.fromFile(PARA.DIR_DATA_FILE + "fc1_b.txt").getLines.size
//    val b_fc1 : Array[SInt] = Array.fill(size_b_fc1)(0.S(PARA.FBIT_WIDTH.W))
//    var count_b_fc1 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "fc1_b.txt").getLines) {
//      b_fc1(count_b_fc1) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_b_fc1 = count_b_fc1 + 1
//    }
//    //======================== LOAD WEIGHT FC2 =============================//
//    val size_w_fc2 = Source.fromFile(PARA.DIR_DATA_FILE + "fc2_w.txt").getLines.size
//    val w_fc2 : Array[SInt] = Array.fill(size_w_fc2)(0.S(PARA.FBIT_WIDTH.W))
//    var count_fc2 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "fc2_w.txt").getLines) {
//      w_fc2(count_fc2) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_fc2 = count_fc2 + 1
//    }
//    //======================== LOAD BIAS FC2 ===============================//
//    val size_b_fc2 = Source.fromFile(PARA.DIR_DATA_FILE + "fc2_b.txt").getLines.size
//    val b_fc2 : Array[SInt] = Array.fill(size_b_fc2)(0.S(PARA.FBIT_WIDTH.W))
//    var count_b_fc2 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "fc2_b.txt").getLines) {
//      b_fc2(count_b_fc2) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_b_fc2 = count_b_fc2 + 1
//    }
//    //======================== LOAD WEIGHT FC3 =============================//
//    val size_w_fc3 = Source.fromFile(PARA.DIR_DATA_FILE + "fc3_w.txt").getLines.size
//    val w_fc3 : Array[SInt] = Array.fill(size_w_fc3)(0.S(PARA.FBIT_WIDTH.W))
//    var count_fc3 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "fc3_w.txt").getLines) {
//      w_fc3(count_fc3) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_fc3 = count_fc3 + 1
//    }
//    //======================== LOAD BIAS FC3  =============================//
//    val size_b_fc3 = Source.fromFile(PARA.DIR_DATA_FILE + "fc3_b.txt").getLines.size
//    val b_fc3 : Array[SInt] = Array.fill(size_b_fc3)(0.S(PARA.FBIT_WIDTH.W))
//    var count_b_fc3 = 0
//    for (line <- Source.fromFile(PARA.DIR_DATA_FILE + "fc3_b.txt").getLines) {
//      b_fc3(count_b_fc3) = (line.toDouble * 256).toInt.asSInt(PARA.FBIT_WIDTH.W)
//      count_b_fc3 = count_b_fc3 + 1
//    }
//    //=====================================================================//
//
//    poke(dut.io.start_in, true.B)
//    poke(dut.io.conv_para_in.stride, 0.U)
//    poke(dut.io.conv_para_in.filter_size, 4.U)
//    poke(dut.io.conv_para_in.matrix_size, 27.U)
//    poke(dut.io.conv_para_in.layer_size, 1.U)
//    poke(dut.io.conv_para_in.dim_in, 0.U)
//    poke(dut.io.conv_para_in.dim_out, 5.U)
//
//    poke(dut.io.pool_para_in.stride, 1.U)
//    poke(dut.io.pool_para_in.filter_size, 1.U)
//    poke(dut.io.pool_para_in.matrix_size, 23.U)
//    poke(dut.io.pool_para_in.layer_size, 1.U)
//    poke(dut.io.pool_para_in.dim_in, 0.U)
//    poke(dut.io.pool_para_in.dim_out, 5.U)
//
//    poke(dut.io.fc_para_in.stride, 0.U)
//    poke(dut.io.fc_para_in.filter_size, 0.U)
//    poke(dut.io.fc_para_in.matrix_size, 0.U)
//    poke(dut.io.fc_para_in.layer_size, 2.U)
//    poke(dut.io.fc_para_in.dim_in, 119.U)
//    poke(dut.io.fc_para_in.dim_out, 83.U)
//    step(1)
//    poke(dut.io.start_in, false.B)
//
//    for (x <- 0 until size_data) {
//      // Generate data
//      tmp_data = data(x)
////      println("Input data: " + tmp_data.toString())
//      poke(dut.io.data_ena, true.B)
//      poke(dut.io.data_in, tmp_data)
//      step(1)
//    }
//    poke(dut.io.data_ena, false.B)
//    poke(dut.io.data_in, BigInt(0))
//
//    var current_pointer_0 = 0
//    for (x <- 0 until size_w_conv1/6) {
//      tmp_weight = w_conv1(x)
////      println("Input weight: " + tmp_weight.toString())
//      poke(dut.io.weight_ena, true.B)
//      poke(dut.io.weight_in, tmp_weight)
//      step(1)
//    }
//    poke(dut.io.weight_ena, false.B)
//    poke(dut.io.weight_in, BigInt(0))
//    current_pointer_0 = current_pointer_0 + (size_w_conv1 / 6)
//
//    for (x <- 0 until size_b_conv1) {
//      tmp_bias = b_conv1(x)
////      println("Input bias: " + tmp_bias.toString())
//      poke(dut.io.bias_ena, true.B)
//      poke(dut.io.bias_in, tmp_bias)
//      step(1)
//    }
//    poke(dut.io.bias_ena, false.B)
//    poke(dut.io.bias_in, BigInt(0))
//
//    poke(dut.io.ready_data_in, true.B)
//    step(1)
//    poke(dut.io.ready_data_in, false.B)
//
//    var current_pointer_1 = 0
//    var current_pointer_2 = 0
//    var current_pointer_3 = 0
//    var current_pointer_4 = 0
//    var current_layer = 0
//
//    for (loop_count <- 0 until 100000) {
//      if (peek(dut.io.done_out) == 1) {
//        if (current_layer == 0) {
//          poke(dut.io.start_in, true.B)
//          poke(dut.io.conv_para_in.stride, 0.U)
//          poke(dut.io.conv_para_in.filter_size, 4.U)
//          poke(dut.io.conv_para_in.matrix_size, 11.U)
//          poke(dut.io.conv_para_in.layer_size, 1.U)
//          poke(dut.io.conv_para_in.dim_in, 5.U)
//          poke(dut.io.conv_para_in.dim_out, 15.U)
//
//          poke(dut.io.pool_para_in.stride, 1.U)
//          poke(dut.io.pool_para_in.filter_size, 1.U)
//          poke(dut.io.pool_para_in.matrix_size, 7.U)
//          poke(dut.io.pool_para_in.layer_size, 1.U)
//          poke(dut.io.pool_para_in.dim_in, 5.U)
//          poke(dut.io.pool_para_in.dim_out, 15.U)
//
//          poke(dut.io.fc_para_in.stride, 0.U)
//          poke(dut.io.fc_para_in.filter_size, 0.U)
//          poke(dut.io.fc_para_in.matrix_size, 0.U)
//          poke(dut.io.fc_para_in.layer_size, 2.U)
//          poke(dut.io.fc_para_in.dim_in, 255.U)
//          poke(dut.io.fc_para_in.dim_out, 119.U)
//          step(1)
//          poke(dut.io.start_in, false.B)
//          current_layer = current_layer + 1
//        } else if (current_layer == 1) {
//          poke(dut.io.start_in, true.B)
//          poke(dut.io.conv_para_in.stride, 0.U)
//          poke(dut.io.conv_para_in.filter_size, 0.U)
//          poke(dut.io.conv_para_in.matrix_size, 3.U)
//          poke(dut.io.conv_para_in.layer_size, 1.U)
//          poke(dut.io.conv_para_in.dim_in, 0.U)
//          poke(dut.io.conv_para_in.dim_out, 0.U)
//
//          poke(dut.io.pool_para_in.stride, 0.U)
//          poke(dut.io.pool_para_in.filter_size, 0.U)
//          poke(dut.io.pool_para_in.matrix_size, 3.U)
//          poke(dut.io.pool_para_in.layer_size, 1.U)
//          poke(dut.io.pool_para_in.dim_in, 0.U)
//          poke(dut.io.pool_para_in.dim_out, 0.U)
//
//          poke(dut.io.fc_para_in.stride, 0.U)
//          poke(dut.io.fc_para_in.filter_size, 0.U)
//          poke(dut.io.fc_para_in.matrix_size, 0.U)
//          poke(dut.io.fc_para_in.layer_size, 2.U)
//          poke(dut.io.fc_para_in.dim_in, 255.U)
//          poke(dut.io.fc_para_in.dim_out, 119.U)
//          step(1)
//          poke(dut.io.start_in, false.B)
//          current_layer = current_layer + 1
//        } else if (current_layer == 2) {
//          poke(dut.io.start_in, true.B)
//          poke(dut.io.conv_para_in.stride, 0.U)
//          poke(dut.io.conv_para_in.filter_size, 0.U)
//          poke(dut.io.conv_para_in.matrix_size, 3.U)
//          poke(dut.io.conv_para_in.layer_size, 1.U)
//          poke(dut.io.conv_para_in.dim_in, 0.U)
//          poke(dut.io.conv_para_in.dim_out, 0.U)
//
//          poke(dut.io.pool_para_in.stride, 0.U)
//          poke(dut.io.pool_para_in.filter_size, 0.U)
//          poke(dut.io.pool_para_in.matrix_size, 3.U)
//          poke(dut.io.pool_para_in.layer_size, 1.U)
//          poke(dut.io.pool_para_in.dim_in, 0.U)
//          poke(dut.io.pool_para_in.dim_out, 0.U)
//
//          poke(dut.io.fc_para_in.stride, 0.U)
//          poke(dut.io.fc_para_in.filter_size, 0.U)
//          poke(dut.io.fc_para_in.matrix_size, 0.U)
//          poke(dut.io.fc_para_in.layer_size, 2.U)
//          poke(dut.io.fc_para_in.dim_in, 119.U)
//          poke(dut.io.fc_para_in.dim_out, 83.U)
//          step(1)
//          poke(dut.io.start_in, false.B)
//          current_layer = current_layer + 1
//        } else {
//          poke(dut.io.start_in, true.B)
//          poke(dut.io.conv_para_in.stride, 0.U)
//          poke(dut.io.conv_para_in.filter_size, 0.U)
//          poke(dut.io.conv_para_in.matrix_size, 3.U)
//          poke(dut.io.conv_para_in.layer_size, 1.U)
//          poke(dut.io.conv_para_in.dim_in, 0.U)
//          poke(dut.io.conv_para_in.dim_out, 0.U)
//
//          poke(dut.io.pool_para_in.stride, 0.U)
//          poke(dut.io.pool_para_in.filter_size, 0.U)
//          poke(dut.io.pool_para_in.matrix_size, 3.U)
//          poke(dut.io.pool_para_in.layer_size, 1.U)
//          poke(dut.io.pool_para_in.dim_in, 0.U)
//          poke(dut.io.pool_para_in.dim_out, 0.U)
//
//          poke(dut.io.fc_para_in.stride, 0.U)
//          poke(dut.io.fc_para_in.filter_size, 0.U)
//          poke(dut.io.fc_para_in.matrix_size, 0.U)
//          poke(dut.io.fc_para_in.layer_size, 2.U)
//          poke(dut.io.fc_para_in.dim_in, 83.U)
//          poke(dut.io.fc_para_in.dim_out, 10.U)
//          step(1)
//          poke(dut.io.start_in, false.B)
//          current_layer = current_layer + 1
//        }
//      } else {
//        if (peek(dut.io.request_data_out) == 1) {
//          if (current_pointer_0 < (size_w_conv1 - size_w_conv1/6)) {
//            for (x <- 0 until (size_w_conv1 / 6)) {
//              tmp_weight = w_conv1(current_pointer_0 + x)
//              poke(dut.io.weight_ena, true.B)
//              poke(dut.io.weight_in, tmp_weight)
//              step(1)
//            }
//            current_pointer_0 = current_pointer_0 + (size_w_conv1 / 6)
//            poke(dut.io.weight_ena, false.B)
//            poke(dut.io.weight_in, BigInt(0))
//          } else if (current_pointer_1 < (size_w_conv2 - size_w_conv2/96)) {
//            for (x <- 0 until (size_w_conv2 / 96)) {
//              tmp_weight = w_conv2(current_pointer_1 + x)
//              poke(dut.io.weight_ena, true.B)
//              poke(dut.io.weight_in, tmp_weight)
//              step(1)
//            }
//            current_pointer_1 = current_pointer_1 + (size_w_conv2 / 96)
//            poke(dut.io.weight_ena, false.B)
//            poke(dut.io.weight_in, BigInt(0))
//          } else if (current_pointer_2 < (size_w_fc1 - size_w_fc1/120)) {
//            for (x <- 0 until (size_w_fc1 / 120)) {
//              tmp_weight = w_fc1(current_pointer_2 + x)
//              poke(dut.io.weight_ena, true.B)
//              poke(dut.io.weight_in, tmp_weight)
//              step(1)
//            }
//            current_pointer_2 = current_pointer_2 + (size_w_fc1 / 120)
//            poke(dut.io.weight_ena, false.B)
//            poke(dut.io.weight_in, BigInt(0))
//          } else if (current_pointer_3 < (size_w_fc2 - size_w_fc1/84)) {
//            for (x <- 0 until (size_w_fc2 / 84)) {
//              tmp_weight = w_fc2(current_pointer_3 + x)
//              poke(dut.io.weight_ena, true.B)
//              poke(dut.io.weight_in, tmp_weight)
//              step(1)
//            }
//            current_pointer_3 = current_pointer_3 + (size_w_fc2 / 120)
//            poke(dut.io.weight_ena, false.B)
//            poke(dut.io.weight_in, BigInt(0))
//          } else if (current_pointer_4 < (size_w_fc3 - size_w_fc3 / 10)) {
//            for (x <- 0 until (size_w_fc3 / 10)) {
//              tmp_weight = w_fc3(current_pointer_4 + x)
//              poke(dut.io.weight_ena, true.B)
//              poke(dut.io.weight_in, tmp_weight)
//              step(1)
//            }
//            current_pointer_4 = current_pointer_4 + (size_w_fc3 / 10)
//            poke(dut.io.weight_ena, false.B)
//            poke(dut.io.weight_in, BigInt(0))
//          } else {
//            step(1)
//          }
//        } else {
//          step(1)
//        }
//      }
//    }
//    //step(50000)
//  }
//}
//
//object TestCNNTopWithMem extends App {
//  chisel3.iotesters.Driver(() => new cnn_top_with_mem(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.DBIT_WIDTH,
//    PARA.FBIT_WIDTH)) {
//    c => new TestCNNTopWithMem(c)
//  }
//}
//
//object TestCNNTopWithMemVerilog extends App {
//  (new ChiselStage).emitVerilog(new cnn_top_with_mem(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.DBIT_WIDTH,
//    PARA.FBIT_WIDTH))
//}
//
//object TestCNNTopWithMemSystemVerilog extends App {
//  (new ChiselStage).emitSystemVerilog(new cnn_top_with_mem(
//    PARA.MATRIX_WIDTH,
//    PARA.FILTER_WIDTH,
//    NUM_EXT_BIT = 5,
//    PARA.DBIT_WIDTH,
//    PARA.FBIT_WIDTH))
//}
//
//class WaveformCNNTopWithMem extends FlatSpec with Matchers {
//  "WaveformCNNTop" should "pass" in {
//    Driver.execute(Array("--generate-vcd-output", "on"), () => new cnn_top_with_mem(
//      PARA.MATRIX_WIDTH,
//      PARA.FILTER_WIDTH,
//      NUM_EXT_BIT = 5,
//      PARA.DBIT_WIDTH,
//      PARA.FBIT_WIDTH)) {
//      c => new TestCNNTopWithMem(c)
//    } should be (true)
//  }
//}
