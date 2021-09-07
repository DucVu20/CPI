import chisel3._
import scala.math._

class fc_layer(FRACTION_WIDTH : Int,
               NUM_CONCURRENT : Int,
               NUM_BIT_IN     : Int,
               BIT_WIDTH_W    : Int,
               BIT_WIDTH_D    : Int,
               NUM_EXT_BIT    : Int) extends Module {
  val MAX : SInt = (pow(2, BIT_WIDTH_D-1).toInt - 1).asSInt()
  val MIN : SInt = (-pow(2, BIT_WIDTH_D-1).toInt).asSInt()

  val io = IO(new Bundle {
    val clear_in   : Bool = Input(Bool())
    val start_in   : Bool = Input(Bool())
    val data_ena   : Bool = Input(Bool())
    val data_in    : SInt = Input(SInt(BIT_WIDTH_D.W))
    val weight_ena : Bool = Input(Bool())
    val weight_in  : SInt = Input(SInt(BIT_WIDTH_W.W))
    val bias_ena   : Bool = Input(Bool())
    val bias_in    : SInt = Input(SInt(BIT_WIDTH_W.W))
    val done_out   : Bool = Output(Bool())
    /*===============================================*/
    val data_size_out_in : UInt = Input(UInt(7.W))
    val data_size_in_in  : UInt = Input(UInt(7.W))
    /*===============================================*/
    val data_done  : Bool = Output(Bool())
    val data_out   : SInt = Output(SInt(BIT_WIDTH_D.W))
  })

  val data_size_in_reg     : UInt = RegInit(0.U(7.W))
  val data_in_counter_reg  : UInt = RegInit(0.U(7.W))
  val data_size_out_reg    : UInt = RegInit(0.U(7.W))
  val data_out_counter_reg : UInt = RegInit(0.U(7.W))

  val data_in_reg   : Vec[SInt] = RegInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.S(BIT_WIDTH_D.W))))
  val weight_in_reg : Vec[SInt] = RegInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.S(BIT_WIDTH_W.W))))
  val bias_in_reg   : SInt = RegInit(0.S(BIT_WIDTH_W.W))

  val sop_reg       : SInt = RegInit(0.S((BIT_WIDTH_W+BIT_WIDTH_D+NUM_EXT_BIT).W))
  val sop_with_bias : SInt = WireInit(0.S((BIT_WIDTH_W+BIT_WIDTH_D+NUM_EXT_BIT).W))

  val mac_en_reg    : Bool = RegInit(false.B)
  val data_done_reg : Bool = RegInit(false.B)
  val data_out_w    : SInt = WireInit(0.S(BIT_WIDTH_D.W))

  when (io.start_in) {
    data_size_in_reg  := io.data_size_in_in
    data_size_out_reg := io.data_size_out_in
  }

  when (io.start_in === true.B || io.clear_in === true.B) {
    data_in_counter_reg := 0.U
  }.elsewhen (io.start_in) {
    data_out_counter_reg := 0.U
  }.otherwise {
    when (io.data_ena) {
      data_in_counter_reg := data_in_counter_reg + 1.U
    }
    when (data_done_reg) {
      data_out_counter_reg := data_out_counter_reg + 1.U
    }
  }

  when (io.weight_ena === true.B && data_in_counter_reg === data_size_in_reg) {
    mac_en_reg := true.B
  }.otherwise {
    mac_en_reg := false.B
  }

  when (io.data_ena) {
    data_in_reg(data_in_counter_reg) := io.data_in
  }.elsewhen (io.clear_in) {
    data_in_reg := RegInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.S(BIT_WIDTH_D.W))))
  }

  when (io.weight_ena) {
    weight_in_reg(data_in_counter_reg) := io.weight_in
  }.elsewhen (io.start_in === true.B || io.clear_in === true.B) {
    weight_in_reg := RegInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.S(BIT_WIDTH_W.W))))
  }

  when (io.bias_ena) {
    bias_in_reg := io.bias_in
  }.elsewhen (io.start_in === true.B || io.clear_in === true.B) {
    bias_in_reg := 0.S
  }

  val mac : vbp = Module(new vbp(
    NUM_BIT_IN,
    BIT_WIDTH_W,
    BIT_WIDTH_D,
    NUM_CONCURRENT,
    NUM_EXT_BIT
  ))

  mac.io.start_in <> mac_en_reg
  mac.io.w        <> weight_in_reg
  mac.io.a        <> data_in_reg

  when (mac.io.done_out) {
    sop_reg := mac.io.sum
  }

  data_done_reg := mac.io.done_out
  sop_with_bias := sop_reg + bias_in_reg

  when ((sop_with_bias >> FRACTION_WIDTH).asSInt() >= MAX) {
    data_out_w := MAX
  }.elsewhen ((sop_with_bias >> FRACTION_WIDTH).asSInt() < MIN) {
    data_out_w := MIN
  }.otherwise {
    data_out_w := (sop_with_bias >> FRACTION_WIDTH).asSInt()
  }

  io.data_done := data_done_reg
  io.data_out  := data_out_w

  when (data_out_counter_reg === data_size_out_reg) {
    io.done_out := true.B
  }.otherwise {
    io.done_out := false.B
  }
}
