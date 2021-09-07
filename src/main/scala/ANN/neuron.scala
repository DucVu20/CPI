import chisel3._
import scala.math._

class neuron(FRACTION_WIDTH : Int,
             NUM_CONCURRENT : Int,
             NUM_BIT_IN     : Int,
             BIT_WIDTH_W    : Int,
             BIT_WIDTH_A    : Int,
             NUM_EXT_BIT    : Int) extends Module {
  val MAX : SInt = (pow(2, BIT_WIDTH_A-1).toInt - 1).asSInt()
  val MIN : SInt = (-pow(2, BIT_WIDTH_A-1).toInt).asSInt()

  val io = IO(new Bundle {
    val clear     : Bool      = Input(Bool())
    val start     : Bool      = Input(Bool())
    val mac_start : Bool      = Input(Bool())
    val activate  : Bool      = Input(Bool())
    val w         : Vec[SInt] = Input(Vec(NUM_CONCURRENT, SInt(BIT_WIDTH_W.W)))
    val a         : Vec[SInt] = Input(Vec(NUM_CONCURRENT, SInt(BIT_WIDTH_A.W)))
    val b         : SInt      = Input(SInt(BIT_WIDTH_W.W))
    val mac_done  : Bool      = Output(Bool())
    val result    : SInt      = Output(SInt(BIT_WIDTH_A.W))
  })

  val mac : vbp = Module(new vbp(NUM_BIT_IN, BIT_WIDTH_W, BIT_WIDTH_A, NUM_CONCURRENT, NUM_EXT_BIT))

  mac.io.start_in <> io.mac_start
  mac.io.w        <> io.w
  mac.io.a        <> io.a
  io.mac_done     <> mac.io.done_out

  val sop_reg       : SInt = RegInit(0.S((BIT_WIDTH_W+BIT_WIDTH_A+NUM_EXT_BIT).W))
  val bias_reg      : SInt = RegInit(0.S(BIT_WIDTH_W.W))

  val sop_with_bias = WireInit(0.S((BIT_WIDTH_A+BIT_WIDTH_W+NUM_EXT_BIT).W))
  val trunc_sum     = WireInit(0.S(BIT_WIDTH_A.W))

  when (io.clear) {
    sop_reg := 0.S((BIT_WIDTH_W+BIT_WIDTH_A+NUM_EXT_BIT).W)
  }.elsewhen (io.start){
    when (mac.io.done_out) {
      sop_reg := sop_reg + mac.io.sum
    }
  }
  sop_with_bias := sop_reg + io.b

  when ((sop_with_bias >> FRACTION_WIDTH).asSInt() >= MAX){
    trunc_sum := MAX
  }.elsewhen ((sop_with_bias >> FRACTION_WIDTH).asSInt() < MIN) {
    trunc_sum := MIN
  }.otherwise {
    trunc_sum := (sop_with_bias >> FRACTION_WIDTH).asSInt()
  }
  val relu : relu = Module(new relu(BIT_WIDTH_A))

  relu.io.ena <> io.activate
  relu.io.in  <> trunc_sum
  io.result   <> relu.io.out
}