import chisel3._
import scala.math._

class cnn_top (MAX_MATRIX_WIDTH : Int,
               MAX_FILTER_WIDTH : Int,
               NUM_EXT_BIT      : Int,
               DBIT_WIDTH       : Int,
               WBIT_WIDTH       : Int) extends Module {
//  val DATA_MEM_SIZE   : Int = pow(2, DATA_SIZE).toInt
//  val WEIGHT_MEM_SIZE : Int = pow(2, WEIGHT_SIZE).toInt
//  val RESULT_MEM_SIZE : Int = pow(2, RESULT_SIZE).toInt

  val io = IO(new Bundle {
    val start_in      : Bool = Input(Bool())
    val data_ena      : Bool = Input(Bool())
    val data_in       : UInt = Input(UInt(DBIT_WIDTH.W))
    val weight_ena    : Bool = Input(Bool())
    val weight_in     : SInt = Input(SInt(WBIT_WIDTH.W))
    val bias_ena      : Bool = Input(Bool())
    val bias_in       : SInt = Input(SInt(WBIT_WIDTH.W))
    val data_ready_in : Bool = Input(Bool())
    /*===============================================*/
    val conv_stride_in      : UInt = Input(UInt(2.W))
    val conv_filter_size_in : UInt = Input(UInt(3.W))
    val conv_matrix_size_in : UInt = Input(UInt(5.W))
    val pool_stride_in      : UInt = Input(UInt(2.W))
    val pool_filter_size_in : UInt = Input(UInt(3.W))
    val pool_matrix_size_in : UInt = Input(UInt(5.W))
    /*===============================================*/
    val result_done : Bool = Output(Bool())
    val result_out  : UInt = Output(UInt(DBIT_WIDTH.W))
    val layer_done  : Bool = Output(Bool())
  })

  val data_mem   = SyncReadMem(1024, UInt(DBIT_WIDTH.W))
  val weight_mem = SyncReadMem(1024, SInt(WBIT_WIDTH.W))
  val bias_mem   = SyncReadMem(64,   SInt(WBIT_WIDTH.W))
  val result_mem = SyncReadMem(1024, UInt(DBIT_WIDTH.W))

  val fc_done_w      : Bool = WireInit(false.B)
  val fc_data_done_w : Bool = WireInit(false.B)

  val rd_data_w   : Bool = WireInit(false.B)
  val wr_data_w   : Bool = WireInit(false.B)
  val rd_weight_w : Bool = WireInit(false.B)
  val wr_weight_w : Bool = WireInit(false.B)
  val rd_bias_w   : Bool = WireInit(false.B)
  val wr_bias_w   : Bool = WireInit(false.B)
  val rd_result_w : Bool = WireInit(false.B)
  val wr_result_w : Bool = WireInit(false.B)

  val rd_data_addr_w   : UInt = WireInit(0.U(10.W))
  val wr_data_addr_w   : UInt = WireInit(0.U(10.W))
  val rd_weight_addr_w : UInt = WireInit(0.U(10.W))
  val wr_weight_addr_w : UInt = WireInit(0.U(10.W))
  val rd_bias_addr_w   : UInt = WireInit(0.U(10.W))
  val wr_bias_addr_w   : UInt = WireInit(0.U(10.W))
  val rd_result_addr_w : UInt = WireInit(0.U(10.W))
  val wr_result_addr_w : UInt = WireInit(0.U(10.W))

  val result_data_w : UInt = WireInit(0.U(DBIT_WIDTH.W))

//  val cnn_controller = Module(new cnn_controller(ADDR_WIDTH = 10))

  val conv_layer = Module(new conv_layer(
    MAX_MATRIX_WIDTH,
    MAX_FILTER_WIDTH,
    NUM_EXT_BIT,
    WBIT_WIDTH,
    DBIT_WIDTH + 1
  ))

  val relu = Module(new relu(BIT_WIDTH = DBIT_WIDTH + 1))

  val pool_layer = Module(new pool_layer(
    MAX_MATRIX_WIDTH - 1, // 32 - 3 + 1 || 3: MIN_FILTER_WIDTH || 32: conv's MAX_MATRIX_WIDTH
    MAX_FILTER_WIDTH,
    DBIT_WIDTH + 1
  ))

  conv_layer.io.start_in   <> io.start_in
  conv_layer.io.data_ena   <> io.data_ena
  conv_layer.io.data_in    <> io.data_in.asSInt()
  conv_layer.io.weight_ena <> io.weight_ena
  conv_layer.io.weight_in  <> io.weight_in

  conv_layer.io.filter_size_in <> io.conv_filter_size_in
  conv_layer.io.matrix_size_in <> io.conv_matrix_size_in
  conv_layer.io.stride_in      <> io.conv_stride_in

  relu.io.ena <> conv_layer.io.data_done
  relu.io.in  <> (conv_layer.io.data_out >> (WBIT_WIDTH + NUM_EXT_BIT - 1)).asSInt()

  pool_layer.io.start_in <> io.start_in
  pool_layer.io.data_ena <> relu.io.done
  pool_layer.io.data_in  <> relu.io.out

  pool_layer.io.filter_size_in <> io.pool_filter_size_in
  pool_layer.io.matrix_size_in <> io.pool_matrix_size_in
  pool_layer.io.stride_in      <> io.pool_stride_in

  io.result_done <> pool_layer.io.data_done
  io.result_out  <> pool_layer.io.data_out
  io.layer_done  <> pool_layer.io.done_out
}
