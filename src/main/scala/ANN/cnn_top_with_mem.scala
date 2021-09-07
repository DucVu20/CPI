import chisel3._

class cnn_top_with_mem (MAX_MATRIX_WIDTH : Int,
                        MAX_FILTER_WIDTH : Int,
                        NUM_EXT_BIT      : Int,
                        DBIT_WIDTH       : Int,
                        WBIT_WIDTH       : Int) extends Module {
  val ADDR_WIDTH : Int = 10

  val io = IO(new Bundle {
    val start_in      : Bool = Input(Bool())
    val data_ena      : Bool = Input(Bool())
    val data_in       : SInt = Input(SInt(DBIT_WIDTH.W))
    val weight_ena    : Bool = Input(Bool())
    val weight_in     : SInt = Input(SInt(WBIT_WIDTH.W))
    val bias_ena      : Bool = Input(Bool())
    val bias_in       : SInt = Input(SInt(WBIT_WIDTH.W))
    /*===============================================*/
    val ready_data_in    : Bool = Input(Bool())
    val request_data_out : Bool = Output(Bool())
    /*===============================================*/
    val conv_para_in = Input(new para_io)
    val pool_para_in = Input(new para_io)
    val fc_para_in   = Input(new para_io)
    /*===============================================*/
    val result_done : Bool = Output(Bool())
    val result_out  : SInt = Output(SInt(DBIT_WIDTH.W))
    val done_out    : Bool = Output(Bool())
  })

  val tmp_wr_data_0_w : SInt = WireInit(0.S(DBIT_WIDTH.W))
  val tmp_wr_data_1_w : SInt = WireInit(0.S(DBIT_WIDTH.W))
  val tmp_wr_weight_0_w : SInt = WireInit(0.S(WBIT_WIDTH.W))
  val tmp_wr_weight_1_w : SInt = WireInit(0.S(WBIT_WIDTH.W))
  val tmp_wr_bias_w : SInt = WireInit(0.S(WBIT_WIDTH.W))

  val conv_data_done_reg : Bool = RegInit(false.B)

//  val cnn_controller = Module(new cnn_controller(ADDR_WIDTH = 11))
  val cnn_controller_with_bundle = Module(new cnn_controller_with_bundle(ADDR_WIDTH = 10))

  val conv_layer = Module(new conv_layer(
    MAX_MATRIX_WIDTH,
    MAX_FILTER_WIDTH,
    NUM_EXT_BIT,
    WBIT_WIDTH,
    DBIT_WIDTH
  ))

  val relu = Module(new relu(BIT_WIDTH = DBIT_WIDTH))

  val pool_layer = Module(new pool_layer(
    MAX_MATRIX_WIDTH,
    MAX_FILTER_WIDTH,
    DBIT_WIDTH
  ))

  val fc_layer = Module(new fc_layer(
    FRACTION_WIDTH = WBIT_WIDTH,
    NUM_CONCURRENT = 16,
    NUM_BIT_IN     = 4,
    BIT_WIDTH_W    = WBIT_WIDTH,
    BIT_WIDTH_D    = DBIT_WIDTH,
    NUM_EXT_BIT    = NUM_EXT_BIT
  ))

  val data_1_mem   = Module(new single_port_ram(1024, SInt(DBIT_WIDTH.W)))
  val data_0_mem   = Module(new single_port_ram(1024, SInt(DBIT_WIDTH.W)))
  val weight_1_mem = Module(new single_port_ram(1024, SInt(WBIT_WIDTH.W)))
  val weight_0_mem = Module(new single_port_ram(1024, SInt(WBIT_WIDTH.W)))
  val bias_mem     = Module(new single_port_ram(1024, SInt(WBIT_WIDTH.W)))

  data_1_mem.io.control <> cnn_controller_with_bundle.io.data_mem_1_ctrl_out
  data_1_mem.io.data_in <> tmp_wr_data_1_w

  data_0_mem.io.control <> cnn_controller_with_bundle.io.data_mem_0_ctrl_out
  data_0_mem.io.data_in <> tmp_wr_data_0_w

  weight_1_mem.io.control <> cnn_controller_with_bundle.io.weight_mem_1_ctrl_out
  weight_1_mem.io.data_in <> tmp_wr_weight_1_w

  weight_0_mem.io.control <> cnn_controller_with_bundle.io.weight_mem_0_ctrl_out
  weight_0_mem.io.data_in <> tmp_wr_weight_0_w

  bias_mem.io.control <> cnn_controller_with_bundle.io.bias_mem_ctrl_out
  bias_mem.io.data_in <> tmp_wr_bias_w

  tmp_wr_data_1_w := Mux(io.data_ena, io.data_in, Mux(cnn_controller_with_bundle.io.data_mem_select_out, Mux(cnn_controller_with_bundle.io.conv_pool_select_out, pool_layer.io.data_out, Mux(cnn_controller_with_bundle.io.fc_select_out, fc_layer.io.data_out, conv_layer.io.data_out)), 0.S))
  tmp_wr_data_0_w := Mux(io.data_ena, io.data_in, Mux(!cnn_controller_with_bundle.io.data_mem_select_out, Mux(cnn_controller_with_bundle.io.conv_pool_select_out, pool_layer.io.data_out, Mux(cnn_controller_with_bundle.io.fc_select_out, fc_layer.io.data_out, conv_layer.io.data_out)), 0.S))

  tmp_wr_weight_1_w := Mux(io.weight_ena, Mux(cnn_controller_with_bundle.io.weight_mem_select_out, io.weight_in, 0.S), 0.S)
  tmp_wr_weight_0_w := Mux(io.weight_ena, Mux(!cnn_controller_with_bundle.io.weight_mem_select_out, io.weight_in, 0.S), 0.S)

  tmp_wr_bias_w := Mux(io.bias_ena, io.bias_in, 0.S)

  cnn_controller_with_bundle.io.start_in      <> io.start_in
  cnn_controller_with_bundle.io.conv_para_in  <> io.conv_para_in
  cnn_controller_with_bundle.io.pool_para_in  <> io.pool_para_in
  cnn_controller_with_bundle.io.fc_para_in    <> io.fc_para_in

  cnn_controller_with_bundle.io.wr_weight_in  <> io.weight_ena
  cnn_controller_with_bundle.io.wr_data_in    <> io.data_ena
  cnn_controller_with_bundle.io.wr_bias_in    <> io.bias_ena
  cnn_controller_with_bundle.io.ready_data_in <> io.ready_data_in

  cnn_controller_with_bundle.io.conv_data_done_in <> conv_layer.io.data_done
  cnn_controller_with_bundle.io.pool_data_done_in <> pool_layer.io.data_done
  cnn_controller_with_bundle.io.pool_done_in      <> pool_layer.io.done_out
  cnn_controller_with_bundle.io.fc_data_done_in   <> fc_layer.io.data_done
  cnn_controller_with_bundle.io.fc_done_in        <> fc_layer.io.done_out

  io.request_data_out <> cnn_controller_with_bundle.io.request_data_out

  conv_layer.io.start_in      <> cnn_controller_with_bundle.io.conv_start_out
  conv_layer.io.data_ena      := Mux(cnn_controller_with_bundle.io.data_mem_select_out, data_0_mem.io.control.en, data_1_mem.io.control.en)
  conv_layer.io.data_in       := Mux(cnn_controller_with_bundle.io.data_mem_select_out, data_0_mem.io.data_out, data_1_mem.io.data_out)
  conv_layer.io.weight_ena    := Mux(cnn_controller_with_bundle.io.weight_mem_select_out, weight_0_mem.io.control.en, weight_1_mem.io.control.en)
  conv_layer.io.weight_in     := Mux(cnn_controller_with_bundle.io.weight_mem_select_out, weight_0_mem.io.data_out, weight_1_mem.io.data_out)
  conv_layer.io.prev_data_ena := Mux(cnn_controller_with_bundle.io.data_mem_select_out, data_1_mem.io.control.en, data_0_mem.io.control.en)
  conv_layer.io.prev_data_in  := Mux(cnn_controller_with_bundle.io.data_mem_select_out, data_1_mem.io.data_out, data_0_mem.io.data_out)
  conv_layer.io.bias_ena      := bias_mem.io.control.en
  conv_layer.io.bias_in       := bias_mem.io.data_out

  conv_layer.io.stride_in      <> cnn_controller_with_bundle.io.conv_para_out.stride
  conv_layer.io.filter_size_in <> cnn_controller_with_bundle.io.conv_para_out.filter_size
  conv_layer.io.matrix_size_in <> cnn_controller_with_bundle.io.conv_para_out.matrix_size

  io.result_done <> fc_layer.io.data_done
  io.result_out  <> fc_layer.io.data_out
  io.done_out    <> cnn_controller_with_bundle.io.done_out

  conv_data_done_reg := conv_layer.io.data_done

  relu.io.ena <> conv_data_done_reg
  relu.io.in  <> conv_layer.io.data_out

  pool_layer.io.start_in <> cnn_controller_with_bundle.io.conv_start_out
  pool_layer.io.data_ena <> relu.io.done
  pool_layer.io.data_in  <> relu.io.out

  pool_layer.io.stride_in      <> cnn_controller_with_bundle.io.pool_para_out.stride
  pool_layer.io.filter_size_in <> cnn_controller_with_bundle.io.pool_para_out.filter_size
  pool_layer.io.matrix_size_in <> cnn_controller_with_bundle.io.pool_para_out.matrix_size

  fc_layer.io.start_in   <> cnn_controller_with_bundle.io.fc_start_out
  fc_layer.io.clear_in   <> cnn_controller_with_bundle.io.clear_fc_out
  fc_layer.io.data_ena   <> Mux(cnn_controller_with_bundle.io.data_mem_select_out, data_0_mem.io.control.en, data_1_mem.io.control.en)
  fc_layer.io.data_in    <> Mux(cnn_controller_with_bundle.io.data_mem_select_out, data_0_mem.io.data_out, data_1_mem.io.data_out)
  fc_layer.io.weight_ena <> Mux(cnn_controller_with_bundle.io.weight_mem_select_out, weight_0_mem.io.control.en, weight_1_mem.io.control.en)
  fc_layer.io.weight_in  <> Mux(cnn_controller_with_bundle.io.weight_mem_select_out, weight_0_mem.io.data_out, weight_1_mem.io.data_out)
  fc_layer.io.bias_ena   <> cnn_controller_with_bundle.io.bias_mem_ctrl_out.en
  fc_layer.io.bias_in    <> bias_mem.io.data_out
  fc_layer.io.data_size_in_in  <> cnn_controller_with_bundle.io.fc_para_out.dim_in
  fc_layer.io.data_size_out_in <> cnn_controller_with_bundle.io.fc_para_out.dim_out

}

