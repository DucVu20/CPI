import chisel3._

class relu(BIT_WIDTH : Int) extends Module {
  val io = IO(new Bundle{
    val ena  : Bool = Input(Bool())
    val in   : SInt = Input(SInt(BIT_WIDTH.W))
    val out  : SInt = Output(SInt(BIT_WIDTH.W))
    val done : Bool = Output(Bool())
  })

  val tmp_reg  : SInt = RegInit(0.S(BIT_WIDTH.W))
  val done_reg : Bool = RegInit(false.B)

  when (io.ena) {
    when(io.in(io.in.getWidth - 1) === false.B) {
      tmp_reg := io.in
    }.otherwise {
      tmp_reg := 0.S
    }
  }

  done_reg := io.ena
  io.done  := done_reg
  io.out   := tmp_reg
}