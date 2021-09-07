import chisel3._
import scala.math._

class pool_layer (MAX_MATRIX_WIDTH  : Int,
                  MAX_FILTER_WIDTH  : Int,
                  BIT_WIDTH         : Int) extends Module {
  val FCTR_BIT_WIDTH : Int = ceil(log(MAX_FILTER_WIDTH)/log(2)).toInt
  val DCTR_BIT_WIDTH : Int = ceil(log(MAX_MATRIX_WIDTH)/log(2)).toInt
  val OUTPUT_WIDTH   : Int = MAX_MATRIX_WIDTH - (MAX_MATRIX_WIDTH % MAX_FILTER_WIDTH)

  def do_max (x : SInt, y : SInt) : SInt = Mux(y < x, x, y)

  val io = IO(new Bundle {
    val start_in  : Bool = Input(Bool())
    val data_ena  : Bool = Input(Bool())
    val data_in   : SInt = Input(SInt(BIT_WIDTH.W))
    /*===============================================*/
    val stride_in      : UInt = Input(UInt(2.W))  // MAX_STRIDE = 4
    val filter_size_in : UInt = Input(UInt(3.W))  // MAX_FILTER_SIZE = 5
    val matrix_size_in : UInt = Input(UInt(5.W))  // MAX_MATRIX_SIZE = 32
//    val pool_size_out  : UInt = Output(UInt(5.W))
    /*===============================================*/
    val data_done : Bool = Output(Bool())
    val data_out  : SInt = Output(SInt(BIT_WIDTH.W))
    val done_out  : Bool = Output(Bool())
  })

  val stride_en_reg  : Bool = RegInit(false.B)
  val data_valid_reg : Bool = RegInit(false.B)
  val data_avail_reg : Bool = RegInit(false.B)
  val data_done_reg  : Bool = RegInit(false.B)

  val counter_width_reg    : UInt = RegInit(0.U(DCTR_BIT_WIDTH.W))
  val counter_height_reg   : UInt = RegInit(0.U(DCTR_BIT_WIDTH.W))
  val max_data_counter_reg : UInt = RegInit(0.U(DCTR_BIT_WIDTH.W))
  val data_counter_reg     : UInt = RegInit(0.U(FCTR_BIT_WIDTH.W))
  
  val stride_counter_width_reg  : UInt = RegInit(0.U(FCTR_BIT_WIDTH.W))
  val stride_counter_height_reg : UInt = RegInit(0.U(FCTR_BIT_WIDTH.W))
  
  val counter_done_width_reg  : UInt = RegInit(0.U(5.W))
  val counter_done_height_reg : UInt = RegInit(0.U(5.W))

  //size of the convoluted layer is ((matrix_width-filter_width)/s+1)x((matrix_width-filter_width)/s+1)
  val data_in_reg     : Vec[SInt] = RegInit(VecInit(Seq.fill(MAX_FILTER_WIDTH)(0.S(BIT_WIDTH.W))))
  val max_data_reg    : Vec[Vec[SInt]] = RegInit(VecInit(Seq.fill(MAX_FILTER_WIDTH)(VecInit(Seq.fill(MAX_MATRIX_WIDTH-MAX_FILTER_WIDTH+1)(0.S(BIT_WIDTH.W))))))
  val result_data_reg : Vec[Vec[SInt]] = RegInit(VecInit(Seq.fill(MAX_FILTER_WIDTH)(VecInit(Seq.fill(MAX_FILTER_WIDTH)(0.S(BIT_WIDTH.W))))))

  val stride_reg      : UInt = RegInit(0.U(2.W))
  val filter_size_reg : UInt = RegInit(0.U(3.W))
  val matrix_size_reg : UInt = RegInit(0.U(5.W))

  val done_out_w    : Bool = WireInit(false.B)
  val pool_size_w   : UInt = WireInit(0.U(12.W))
  val pool_size_reg : UInt = RegInit(0.U(5.W))

  val data_out_reg : SInt = RegInit(0.S(BIT_WIDTH.W))
  // ====================================================== //
  // =============== PARAMETER REGISTERS ================== //
  when (io.start_in) {
    pool_size_w := io.matrix_size_in - io.filter_size_in
  }.otherwise {
    pool_size_w := 0.U
  }

  when (io.start_in) {
    stride_reg      := io.stride_in
    filter_size_reg := io.filter_size_in
    matrix_size_reg := io.matrix_size_in

    when (io.stride_in === 0.U(2.W)) {  //s=1 in dec
      pool_size_reg := pool_size_w
    }.elsewhen (io.stride_in === 1.U(2.W)) {  // s=2 in dec
      pool_size_reg := (pool_size_w >> 1).asUInt()
    }.elsewhen (io.stride_in === 2.U(2.W)) {
      // MAGIC by Montgomery division || x * 0x56 >> 8 || x * 0x55555556 >> 32
      pool_size_reg := (((pool_size_w << 6).asUInt() + (pool_size_w << 4).asUInt() + (pool_size_w << 2).asUInt() + (pool_size_w << 1).asUInt()) >> 8).asUInt()
    }.otherwise {
      pool_size_reg := (pool_size_w >> 2).asUInt()
    }
  }
//  io.pool_size_out := pool_size_reg
  // ====================================================== //
  // =============== STRIDE REGISTERS ===================== //
  // filter_size_reg = actual_filter_size - 1
  // stride_reg = actual_stride - 1
  when (io.data_ena) {
    when (counter_width_reg >= filter_size_reg) {
      stride_en_reg := true.B
    }.elsewhen (counter_width_reg === 0.U) {
      stride_en_reg := false.B
    }
  }.otherwise {
    stride_en_reg := false.B
  }

  when (stride_en_reg) {
    when (stride_counter_width_reg === stride_reg) {
      stride_counter_width_reg := 0.U(DCTR_BIT_WIDTH.W)
    }.otherwise {
      stride_counter_width_reg := stride_counter_width_reg + 1.U
    }
  }.elsewhen (io.start_in || counter_width_reg === 0.U) {
    stride_counter_width_reg := 0.U(DCTR_BIT_WIDTH.W)
  }

  // ====================================================== //
  // =============== DATA COUNTER REGISTER ================ //
  when (data_done_reg) {
    when (counter_done_width_reg === pool_size_reg) {
      counter_done_width_reg := 0.U(5.W)
    }.otherwise {
      counter_done_width_reg := counter_done_width_reg + 1.U
    }

  }.elsewhen (io.start_in) {
    counter_done_width_reg  := 0.U(5.W)
    counter_done_height_reg := 0.U(5.W)
  }

  when (stride_en_reg && stride_counter_width_reg === 0.U) {
    when (max_data_counter_reg === pool_size_reg) {
      max_data_counter_reg := 0.U(DCTR_BIT_WIDTH.W)

      when (data_avail_reg) {
        when(stride_counter_height_reg === stride_reg) {
          stride_counter_height_reg := 0.U(FCTR_BIT_WIDTH.W)
        }.otherwise {
          stride_counter_height_reg := stride_counter_height_reg + 1.U
        }
      }
    }.otherwise {
      max_data_counter_reg := max_data_counter_reg + 1.U
    }
  }.elsewhen (io.start_in) {
    max_data_counter_reg      := 0.U
    stride_counter_height_reg := 0.U
  }

  when (io.data_ena) {
    when (data_counter_reg === filter_size_reg || counter_width_reg === matrix_size_reg) {
      data_counter_reg := 0.U(FCTR_BIT_WIDTH.W)
    }.otherwise {
      data_counter_reg := data_counter_reg + 1.U
    }
  }.elsewhen (io.start_in) {
    data_counter_reg   := 0.U(FCTR_BIT_WIDTH.W)
  }

  when (io.data_ena) {
    when (counter_width_reg === matrix_size_reg) {
      counter_width_reg := 0.U(DCTR_BIT_WIDTH.W)
    }.otherwise {
      counter_width_reg := counter_width_reg + 1.U
    }

    when (counter_width_reg === matrix_size_reg) {
      when (counter_height_reg === filter_size_reg) {
        counter_height_reg := 0.U(DCTR_BIT_WIDTH.W)
      }.otherwise {
        counter_height_reg := counter_height_reg + 1.U
      }
    }
  }.elsewhen (io.start_in) {
    counter_width_reg  := 0.U(DCTR_BIT_WIDTH.W)
    counter_height_reg := 0.U(DCTR_BIT_WIDTH.W)
  }

  // ====================================================== //
  // =============== DATA REGISTER ======================== //
  when (io.data_ena) {
    // data_in_reg(counter_height_reg) := do_max(data_in_reg(counter_height_reg), io.data_in)
    data_in_reg(data_counter_reg) := io.data_in
  }.elsewhen (io.start_in) {
    data_in_reg := RegInit(VecInit(Seq.fill(MAX_FILTER_WIDTH)(0.S(BIT_WIDTH.W))))
  }

  when (stride_en_reg && stride_counter_width_reg === 0.U) {
    max_data_reg(counter_height_reg)(max_data_counter_reg) := (0 until MAX_FILTER_WIDTH).map(data_in_reg(_)).reduce(do_max)
  }.elsewhen (io.start_in) {
    max_data_reg := RegInit(VecInit(Seq.fill(MAX_FILTER_WIDTH)(VecInit(Seq.fill(MAX_MATRIX_WIDTH-MAX_FILTER_WIDTH+1)(0.S(BIT_WIDTH.W))))))
  }

  // ====================================================== //
  // =============== OUTPUTS ============================== //
  when (data_done_reg
    && counter_done_width_reg === pool_size_reg
    && counter_done_height_reg === pool_size_reg) {
    done_out_w := true.B
  }.otherwise {
    done_out_w := false.B
  }

  io.done_out := done_out_w

  when (io.start_in || done_out_w) {
    data_avail_reg := false.B
  }.elsewhen (counter_height_reg === filter_size_reg
    && counter_done_height_reg < pool_size_reg) {
    data_avail_reg := true.B
  }

  when (io.start_in || stride_counter_height_reg =/= 0.U) {
    data_valid_reg := false.B
  }.elsewhen (data_avail_reg && stride_counter_height_reg === 0.U) {
    data_valid_reg := true.B
  }

  when (stride_en_reg && data_valid_reg) {
    when (stride_counter_width_reg === 0.U) {
      data_done_reg := true.B
      data_out_reg := (0 until MAX_FILTER_WIDTH).map(max_data_reg(_)(max_data_counter_reg)).reduce(do_max)
    }.otherwise {
      data_done_reg := false.B
    }
  }.otherwise {
    data_done_reg := false.B
  }

  io.data_out := data_out_reg

  io.data_done := data_done_reg
}
