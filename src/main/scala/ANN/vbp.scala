import chisel3._
import chisel3.util._

class vbp(NUM_BIT_IN      : Int,
          BIT_WIDTH_W     : Int,
          BIT_WIDTH_A     : Int,
          NUM_CONCURRENT  : Int,
          NUM_EXT_BIT     : Int) extends Module {
  val io = IO(new Bundle {
    val start_in : Bool      = Input(Bool())
    val w        : Vec[SInt] = Input(Vec(NUM_CONCURRENT, SInt(BIT_WIDTH_W.W)))
    val a        : Vec[SInt] = Input(Vec(NUM_CONCURRENT, SInt(BIT_WIDTH_A.W)))
    val done_out : Bool      = Output(Bool())
    val sum      : SInt      = Output(SInt((BIT_WIDTH_W + BIT_WIDTH_A + NUM_EXT_BIT).W))
  })

  def do_absolute(x : SInt) : SInt = Mux(x < 0.S, 0.S - x, x)

  val counter_reg    : UInt = RegInit(0.U(NUM_BIT_IN.W))
  val sum_reg        : SInt = RegInit(0.S((BIT_WIDTH_A + BIT_WIDTH_W + NUM_EXT_BIT).W))

  val w_reg : Vec[SInt] = RegInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.S(BIT_WIDTH_W.W))))
  val a_reg : Vec[SInt] = RegInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.S(BIT_WIDTH_A.W))))

  val sum_next     = WireInit(0.S((BIT_WIDTH_A + BIT_WIDTH_W + NUM_EXT_BIT).W))
  val p_sum        = WireInit(0.S((BIT_WIDTH_A + BIT_WIDTH_W + NUM_EXT_BIT).W))
  val ops          = WireInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.U(1.W))))
  val counter_next = WireInit(0.U(NUM_BIT_IN.W))
  val partial_sum  = WireInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.S((BIT_WIDTH_A + NUM_EXT_BIT).W))))

  val counter_en = WireInit(false.B)
  val done       = WireInit(false.B)

  val idle_state :: calculate_state :: Nil = Enum(2)

  val state_reg = RegInit(idle_state)

  switch (state_reg) {
    //================== IDLE STATE ====================//
    is (idle_state) {
      when (io.start_in) {
        state_reg := calculate_state
      }.otherwise {
        state_reg := idle_state
      }
    }
    //================== CALCULATE STATE ===============//
    is (calculate_state) {
      when (counter_reg === (BIT_WIDTH_W-2).U) {
        state_reg := idle_state
      }.otherwise {
        state_reg := calculate_state
      }
    }
  }

  when (state_reg === idle_state) {
    counter_en := false.B
    done       := false.B
  }.elsewhen (state_reg === calculate_state) {
    counter_en := true.B
    when (counter_reg === (BIT_WIDTH_W-2).U) {
      done     := true.B
    }.otherwise {
      done     := false.B
    }
  }.otherwise {
    counter_en := false.B
    done       := false.B
  }

  when (io.start_in) {
    counter_reg := 0.U(NUM_BIT_IN.W)
    sum_reg     := (0 until NUM_CONCURRENT).map(partial_sum(_)).reduce(_ + _)
  }.elsewhen(counter_en) {
    counter_reg := counter_next
    sum_reg     := sum_next
  }

  when (counter_reg < (BIT_WIDTH_W - 1).U) {
    counter_next := counter_reg + 1.U
  }.otherwise {
    counter_next := 0.U(NUM_EXT_BIT.W)
  }

  for (i <- 0 until NUM_CONCURRENT) {
    when (io.w(i) < 0.S) {
      a_reg(i) := 0.S - io.a(i)
    }.otherwise {
      a_reg(i) := io.a(i)
    }

    when(io.start_in === 1.U) {
      w_reg(i) := do_absolute(io.w(i))
      ops(i)   := io.w(i)(0)

      when (ops(i) === 1.U) {
        when (io.w(i) < 0.S) {
          partial_sum(i) := 0.S - io.a(i)
        }.otherwise {
          partial_sum(i) := io.a(i)
        }
      }.otherwise {
        partial_sum(i) := 0.S
      }
    }.otherwise {
      w_reg(i) := (w_reg(i) >> 1).asSInt()
      ops(i)   := w_reg(i)(1)

      when (ops(i) === 1.U) {
        partial_sum(i) := a_reg(i)
      }.otherwise {
        partial_sum(i) := 0.S
      }
    }
  }

  p_sum       := ((0 until NUM_CONCURRENT).map(partial_sum(_)).reduce(_ + _) << (counter_reg + 1.U)).asSInt()
  sum_next    := sum_reg + p_sum
  io.done_out := done
  when (done) {
    io.sum    := sum_reg
  }.otherwise {
    io.sum    := 0.S((BIT_WIDTH_W+BIT_WIDTH_A+NUM_EXT_BIT).W)
  }
}
