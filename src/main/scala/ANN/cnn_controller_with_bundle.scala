import chisel3._
import chisel3.util._
object HelloScala extends App{
  println("Hello Chisel World!")
}
class cnn_controller_with_bundle(ADDR_WIDTH : Int) extends Module {
  def gen_counter(max_value : UInt, ena : Bool, clear : Bool): UInt = {
    val counter_reg = RegInit(0.U(max_value.getWidth.W))
    counter_reg := Mux(clear, 0.U, Mux(ena, Mux(counter_reg === max_value, 0.U, counter_reg + 1.U), counter_reg))
    counter_reg
  }
  def gen_counter(max_value : UInt, ena : Bool, clear : Bool, init_value : UInt): UInt = {
    val counter_reg = RegInit(0.U(max_value.getWidth.W))
    counter_reg := Mux(clear, init_value, Mux(ena, Mux(counter_reg === max_value, 0.U, counter_reg + 1.U), counter_reg))
    counter_reg
  }

  val io = IO(new Bundle {
    val start_in : Bool = Input(Bool())
    val done_out : Bool = Output(Bool())
    // =============================================== //
    val conv_para_in  : para_io = Input(new para_io)
    val pool_para_in  : para_io = Input(new para_io)
    val fc_para_in    : para_io = Input(new para_io)
    val conv_para_out : para_io = Output(new para_io)
    val pool_para_out : para_io = Output(new para_io)
    val fc_para_out   : para_io = Output(new para_io)
    // =============================================== //
    val data_mem_1_ctrl_out   = Output(mem_control_io(ADDR_WIDTH))
    val data_mem_0_ctrl_out   = Output(mem_control_io(ADDR_WIDTH))
    val weight_mem_1_ctrl_out = Output(mem_control_io(ADDR_WIDTH))
    val weight_mem_0_ctrl_out = Output(mem_control_io(ADDR_WIDTH))
    val bias_mem_ctrl_out     = Output(mem_control_io(ADDR_WIDTH))
    // =============================================== //
    val wr_weight_in : Bool = Input(Bool())
    val wr_data_in   : Bool = Input(Bool())
    val wr_bias_in   : Bool = Input(Bool())
    // =============================================== //
    val ready_data_in         : Bool = Input(Bool())
    val request_data_out      : Bool = Output(Bool())
    val data_mem_select_out   : Bool = Output(Bool())
    val weight_mem_select_out : Bool = Output(Bool())
    val conv_pool_select_out  : Bool = Output(Bool())
    val fc_select_out         : Bool = Output(Bool())
    val clear_fc_out          : Bool = Output(Bool())
    // =============================================== //
    val conv_data_done_in : Bool = Input(Bool())
    val pool_data_done_in : Bool = Input(Bool())
    val fc_data_done_in   : Bool = Input(Bool())
    // =============================================== //
    val conv_start_out : Bool = Output(Bool())
    val pool_done_in   : Bool = Input(Bool())
    val fc_start_out   : Bool = Output(Bool())
    val fc_done_in     : Bool = Input(Bool())
  })

  val idle_state :: load_data_state :: conv_pool_state :: conv_switch_filter_state :: conv_switch_layer_state :: fully_connected_state :: fc_switch_layer_state :: done_state :: Nil = Enum(8)

  val state_reg = RegInit(idle_state)

  val rd_weight_0_w : Bool = WireInit(false.B)
  val rd_weight_1_w : Bool = WireInit(false.B)
  val wr_weight_0_w : Bool = WireInit(false.B)
  val wr_weight_1_w : Bool = WireInit(false.B)
  val rd_data_0_w   : Bool = WireInit(false.B)
  val rd_data_1_w   : Bool = WireInit(false.B)
  val wr_data_0_w   : Bool = WireInit(false.B)
  val wr_data_1_w   : Bool = WireInit(false.B)
  val rd_bias_w     : Bool = WireInit(false.B)
  val wr_bias_w     : Bool = WireInit(false.B)

  val clear_weight_addr_w : Bool = WireInit(false.B)
  val clear_data_addr_w   : Bool = WireInit(false.B)

  val conv_start_w : Bool = WireInit(false.B)
  val fc_start_w   : Bool = WireInit(false.B)

  val data_mem_select_reg   : Bool = RegInit(false.B)
  val weight_mem_select_reg : Bool = RegInit(false.B)
  val request_data_reg      : Bool = RegInit(false.B)

  val done_w : Bool = WireInit(false.B)

  val conv_para_reg : para_io = Reg(new para_io)
  val pool_para_reg : para_io = Reg(new para_io)
  val fc_para_reg   : para_io = Reg(new para_io)

  val data_base_addr_1_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val data_base_addr_0_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))  
  val conv_data_done_reg : Bool = RegInit(false.B)

  val rd_addr_weight_0_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val rd_addr_weight_1_reg : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val wr_addr_weight_0_reg : UInt = gen_counter(1023.U, wr_weight_0_w, clear_weight_addr_w)
  val wr_addr_weight_1_reg : UInt = gen_counter(1023.U, wr_weight_1_w, clear_weight_addr_w)
  val rd_addr_data_0_reg   : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val rd_addr_data_1_reg   : UInt = RegInit(0.U(ADDR_WIDTH.W))
  val wr_addr_data_0_reg   : UInt = gen_counter(1023.U, wr_data_0_w, clear_data_addr_w, data_base_addr_0_reg)
  val wr_addr_data_1_reg   : UInt = gen_counter(1023.U, wr_data_1_w, clear_data_addr_w, data_base_addr_1_reg)
  val rd_addr_bias_reg     : UInt = gen_counter(1023.U, rd_bias_w, clear_weight_addr_w)
  val wr_addr_bias_reg     : UInt = gen_counter(1023.U, wr_bias_w, clear_weight_addr_w)

  val dim_in_counter_of_conv_reg   : UInt = gen_counter(conv_para_reg.dim_in, state_reg === conv_switch_filter_state, state_reg === idle_state)
  val dim_out_counter_of_conv_reg  : UInt = gen_counter(conv_para_reg.dim_out, state_reg === conv_switch_filter_state && dim_in_counter_of_conv_reg === conv_para_reg.dim_in, state_reg === idle_state)
  val layer_counter_of_conv_reg    : UInt = gen_counter(7.U, state_reg === conv_switch_layer_state, state_reg === idle_state)
  val dim_in_counter_of_fc_reg     : UInt = gen_counter(fc_para_reg.dim_in, state_reg === fully_connected_state, state_reg === fc_switch_layer_state)
  val dim_out_counter_of_fc_reg    : UInt = gen_counter(fc_para_reg.dim_out, state_reg === fc_switch_layer_state, state_reg === done_state)
  val layer_counter_of_fc_reg      : UInt = gen_counter(7.U, state_reg === fc_switch_layer_state, state_reg === idle_state)
  val width_counter_of_weight_reg  : UInt = gen_counter(conv_para_reg.filter_size, Mux(weight_mem_select_reg, rd_weight_0_w, rd_weight_1_w), io.pool_done_in)
  val height_counter_of_weight_reg : UInt = gen_counter(7.U, Mux(weight_mem_select_reg, rd_weight_0_w, rd_weight_1_w) === true.B && width_counter_of_weight_reg === conv_para_reg.filter_size, io.pool_done_in)
  val filter_counter_of_data_reg   : UInt = gen_counter(conv_para_reg.filter_size, Mux(data_mem_select_reg, rd_data_0_w, rd_data_1_w), io.pool_done_in)
  val width_counter_of_data_reg    : UInt = gen_counter(conv_para_reg.matrix_size, Mux(data_mem_select_reg, rd_data_0_w, rd_data_1_w) && (filter_counter_of_data_reg === conv_para_reg.filter_size), io.pool_done_in)
  val height_counter_of_data_reg   : UInt = gen_counter(31.U, Mux(data_mem_select_reg, rd_data_0_w, rd_data_1_w) === true.B && width_counter_of_data_reg === conv_para_reg.matrix_size && filter_counter_of_data_reg === conv_para_reg.filter_size, io.pool_done_in)

  when (io.start_in) {
    conv_para_reg := io.conv_para_in
    pool_para_reg := io.pool_para_in
    fc_para_reg   := io.fc_para_in
  }

  io.conv_para_out := conv_para_reg
  io.pool_para_out := pool_para_reg
  io.fc_para_out   := fc_para_reg

  switch (state_reg) {
    is (idle_state) {
      state_reg := Mux(io.start_in, load_data_state, idle_state)
    }
    is (load_data_state) {
      state_reg := Mux(io.ready_data_in, conv_pool_state, load_data_state)
    }
    is (conv_pool_state) {
      state_reg := Mux(io.pool_done_in, conv_switch_filter_state, conv_pool_state)
    }
    is (conv_switch_filter_state) {
      state_reg := Mux(dim_in_counter_of_conv_reg === conv_para_reg.dim_in && dim_out_counter_of_conv_reg === conv_para_reg.dim_out, conv_switch_layer_state, conv_pool_state)
    }
    is (conv_switch_layer_state) {
      state_reg := Mux(layer_counter_of_conv_reg === conv_para_reg.layer_size, fully_connected_state, conv_pool_state)
    }
    is (fully_connected_state) {
      state_reg := Mux(io.fc_done_in, fc_switch_layer_state, fully_connected_state)
    }
    is (fc_switch_layer_state) {
      state_reg := Mux(layer_counter_of_fc_reg === fc_para_reg.layer_size, done_state, fully_connected_state)
    }
    is (done_state) {
      state_reg := idle_state
    }
  }

  when (data_mem_select_reg) {
    when (state_reg === conv_switch_filter_state && dim_in_counter_of_conv_reg === conv_para_reg.dim_in) {
      when (dim_out_counter_of_conv_reg === conv_para_reg.dim_out) {
        data_base_addr_1_reg := 0.U
      }.otherwise {
        data_base_addr_1_reg := wr_addr_data_1_reg
      }
    }.elsewhen (state_reg === conv_switch_layer_state) {
      data_base_addr_1_reg := 0.U
    }
    rd_addr_data_1_reg := Mux(clear_data_addr_w, data_base_addr_1_reg, Mux(rd_data_1_w, rd_addr_data_1_reg + 1.U, rd_addr_data_1_reg))

    when (rd_data_0_w) {
      when (filter_counter_of_data_reg === conv_para_reg.filter_size) {
        when (width_counter_of_data_reg === conv_para_reg.matrix_size
          && height_counter_of_data_reg === conv_para_reg.matrix_size - conv_para_reg.filter_size) {
          rd_addr_data_0_reg := 0.U
          data_base_addr_0_reg := 0.U
        }.otherwise {
          rd_addr_data_0_reg := data_base_addr_0_reg + 1.U
          data_base_addr_0_reg := data_base_addr_0_reg + 1.U
        }
      }.otherwise {
        rd_addr_data_0_reg := rd_addr_data_0_reg + conv_para_reg.matrix_size + 1.U
      }
    }
  }.otherwise {
    when (state_reg === conv_switch_filter_state && dim_in_counter_of_conv_reg === conv_para_reg.dim_in) {
      when (dim_out_counter_of_conv_reg === conv_para_reg.dim_out) {
        data_base_addr_0_reg := 0.U
      }.otherwise {
        data_base_addr_0_reg := wr_addr_data_0_reg
      }
    }.elsewhen (state_reg === conv_switch_layer_state) {
      data_base_addr_0_reg := 0.U
    }
    rd_addr_data_0_reg := Mux(clear_data_addr_w, data_base_addr_0_reg, Mux(rd_data_0_w, rd_addr_data_0_reg + 1.U, rd_addr_data_0_reg))

    when (rd_data_1_w) {
      when(filter_counter_of_data_reg === conv_para_reg.filter_size) {
        when(width_counter_of_data_reg === conv_para_reg.matrix_size
          && height_counter_of_data_reg === conv_para_reg.matrix_size - conv_para_reg.filter_size) {
          rd_addr_data_1_reg   := 0.U
          data_base_addr_1_reg := 0.U
        }.otherwise {
          rd_addr_data_1_reg   := data_base_addr_1_reg + 1.U
          data_base_addr_1_reg := data_base_addr_1_reg + 1.U
        }
      }.otherwise {
        rd_addr_data_1_reg := rd_addr_data_1_reg + conv_para_reg.matrix_size + 1.U
      }
    }
  }

  when (weight_mem_select_reg) {
    when(rd_weight_0_w) {
      when(width_counter_of_weight_reg === conv_para_reg.filter_size) {
        when(height_counter_of_weight_reg === conv_para_reg.filter_size) {
          rd_addr_weight_0_reg := rd_addr_weight_0_reg + 1.U
        }.otherwise {
          rd_addr_weight_0_reg := Mux(data_mem_select_reg, data_base_addr_0_reg + 1.U, data_base_addr_1_reg + 1.U)
        }
      }.otherwise {
        rd_addr_weight_0_reg := rd_addr_weight_0_reg + conv_para_reg.filter_size + 1.U
      }
    }.elsewhen(clear_weight_addr_w) {
      rd_addr_weight_0_reg := 0.U
    }

    rd_addr_weight_1_reg := 0.U
  }.otherwise {
    rd_addr_weight_0_reg := 0.U

    when (rd_weight_1_w) {
      when (width_counter_of_weight_reg === conv_para_reg.filter_size) {
        when (height_counter_of_weight_reg === conv_para_reg.filter_size) {
          rd_addr_weight_1_reg := rd_addr_weight_1_reg + 1.U
        }.otherwise {
          rd_addr_weight_1_reg := Mux(data_mem_select_reg, data_base_addr_0_reg + 1.U, data_base_addr_1_reg + 1.U)
        }
      }.otherwise {
        rd_addr_weight_1_reg := rd_addr_weight_1_reg + conv_para_reg.filter_size + 1.U
      }
    }.elsewhen (clear_weight_addr_w) {
      rd_addr_weight_1_reg := 0.U
    }
  }

  when ((state_reg === conv_switch_layer_state) || (state_reg === fc_switch_layer_state)) {
    data_mem_select_reg := ~data_mem_select_reg
  }

  when ((state_reg === conv_switch_filter_state) || (state_reg === fc_switch_layer_state) || io.ready_data_in) {
    weight_mem_select_reg := ~weight_mem_select_reg
  }

  request_data_reg   := io.start_in ^ conv_start_w ^ fc_start_w
  conv_data_done_reg := io.conv_data_done_in

  when (state_reg === done_state || state_reg === fc_switch_layer_state) {
    done_w := true.B
  }.elsewhen (state_reg === conv_switch_filter_state) {
    when (dim_out_counter_of_conv_reg === conv_para_reg.dim_out
      && dim_in_counter_of_conv_reg === conv_para_reg.dim_in) {
      done_w := true.B
    }.otherwise {
      done_w := false.B
    }
  }.otherwise {
    done_w := false.B
  }

  when (state_reg === load_data_state) {
    conv_start_w := Mux(io.ready_data_in, true.B, false.B)
  }.elsewhen (state_reg === conv_switch_filter_state) {
    conv_start_w := Mux(dim_in_counter_of_conv_reg === conv_para_reg.dim_in && dim_out_counter_of_conv_reg === conv_para_reg.dim_out, false.B, true.B)
  }.elsewhen (state_reg === conv_switch_layer_state) {
    conv_start_w := Mux(layer_counter_of_conv_reg === conv_para_reg.layer_size, false.B, true.B)
  }.otherwise {
    conv_start_w := false.B
  }

  when (state_reg === conv_switch_layer_state) {
    fc_start_w := Mux(layer_counter_of_conv_reg === conv_para_reg.layer_size, true.B, false.B)
  }.elsewhen (state_reg === fc_switch_layer_state) {
    fc_start_w := Mux(layer_counter_of_fc_reg === fc_para_reg.layer_size, false.B, true.B)
  }.otherwise {
    fc_start_w := false.B
  }

  when (state_reg === conv_switch_filter_state) {
    clear_data_addr_w   := Mux(dim_in_counter_of_conv_reg === conv_para_reg.dim_in, Mux(dim_out_counter_of_conv_reg === conv_para_reg.dim_out, true.B, false.B), true.B)
    clear_weight_addr_w := true.B
    io.clear_fc_out     := false.B
  }.elsewhen (state_reg === conv_switch_layer_state) {
    clear_data_addr_w   := true.B
    clear_weight_addr_w := true.B
    io.clear_fc_out     := false.B
  }.elsewhen (state_reg === fc_switch_layer_state) {
    clear_data_addr_w   := true.B
    clear_weight_addr_w := true.B
    io.clear_fc_out     := true.B
  }.otherwise {
    clear_data_addr_w   := false.B
    clear_weight_addr_w := false.B
    io.clear_fc_out     := false.B
  }

  when (state_reg === load_data_state) {
    rd_data_0_w   := false.B
    rd_data_1_w   := false.B
    wr_data_0_w   := Mux(data_mem_select_reg, io.wr_data_in, false.B)
    wr_data_1_w   := Mux(data_mem_select_reg, false.B, io.wr_data_in)
    rd_weight_0_w := false.B
    rd_weight_1_w := false.B
    wr_weight_0_w := Mux(!weight_mem_select_reg, io.wr_weight_in, false.B)
    wr_weight_1_w := Mux(weight_mem_select_reg, io.wr_weight_in, false.B)
    rd_bias_w     := false.B
    wr_bias_w     := io.wr_bias_in
  }.elsewhen (state_reg === conv_pool_state) {
    rd_data_0_w   := Mux(data_mem_select_reg, Mux(height_counter_of_data_reg === conv_para_reg.matrix_size - conv_para_reg.filter_size + 1.U, false.B, true.B), Mux(dim_in_counter_of_conv_reg =/= 0.U, io.conv_data_done_in, false.B))
    rd_data_1_w   := Mux(!data_mem_select_reg, Mux(height_counter_of_data_reg === conv_para_reg.matrix_size - conv_para_reg.filter_size + 1.U, false.B, true.B), Mux(dim_in_counter_of_conv_reg =/= 0.U, io.conv_data_done_in, false.B))
    wr_data_0_w   := Mux(!data_mem_select_reg, Mux(dim_in_counter_of_conv_reg === conv_para_reg.dim_in, io.pool_data_done_in, conv_data_done_reg), false.B)
    wr_data_1_w   := Mux(data_mem_select_reg, Mux(dim_in_counter_of_conv_reg === conv_para_reg.dim_in, io.pool_data_done_in, conv_data_done_reg), false.B)
    rd_weight_0_w := Mux(weight_mem_select_reg, Mux(height_counter_of_weight_reg === conv_para_reg.filter_size + 1.U, false.B, true.B), false.B)
    rd_weight_1_w := Mux(!weight_mem_select_reg, Mux(height_counter_of_weight_reg === conv_para_reg.filter_size + 1.U, false.B, true.B), false.B)
    wr_weight_0_w := Mux(!weight_mem_select_reg, io.wr_weight_in, false.B)
    wr_weight_1_w := Mux(weight_mem_select_reg, io.wr_weight_in, false.B)
    rd_bias_w     := conv_start_w
    wr_bias_w     := io.wr_bias_in
  }.elsewhen (state_reg === fully_connected_state) {
    rd_data_0_w   := Mux(data_mem_select_reg, Mux(dim_in_counter_of_conv_reg === fc_para_reg.dim_in, false.B, true.B), false.B)
    rd_data_1_w   := Mux(!data_mem_select_reg, Mux(dim_in_counter_of_conv_reg === fc_para_reg.dim_in, false.B, true.B), false.B)
    wr_data_0_w   := Mux(!data_mem_select_reg, io.fc_data_done_in, false.B)
    wr_data_1_w   := Mux(data_mem_select_reg, io.fc_data_done_in, false.B)
    rd_weight_0_w := Mux(weight_mem_select_reg, Mux(dim_in_counter_of_conv_reg === fc_para_reg.dim_in, false.B, true.B), false.B)
    rd_weight_1_w := Mux(!weight_mem_select_reg, Mux(dim_in_counter_of_conv_reg === fc_para_reg.dim_in, false.B, true.B), false.B)
    wr_weight_0_w := Mux(!weight_mem_select_reg, io.wr_weight_in, false.B)
    wr_weight_1_w := Mux(weight_mem_select_reg, io.wr_weight_in, false.B)
    rd_bias_w     := fc_start_w
    wr_bias_w     := io.wr_bias_in
  }.otherwise {
    rd_data_0_w   := false.B
    rd_data_1_w   := false.B
    wr_data_0_w   := false.B
    wr_data_1_w   := false.B
    rd_weight_0_w := false.B
    rd_weight_1_w := false.B
    wr_weight_0_w := false.B
    wr_weight_1_w := false.B
    rd_bias_w     := false.B
    wr_bias_w     := false.B
  }

  when (state_reg === fully_connected_state) {
    io.fc_select_out := true.B
  }.otherwise {
    io.fc_select_out := false.B
  }

  io.data_mem_0_ctrl_out.en   := rd_data_0_w
  io.data_mem_0_ctrl_out.we   := wr_data_0_w
  io.data_mem_0_ctrl_out.addr := Mux(wr_data_0_w, wr_addr_data_0_reg, rd_addr_data_0_reg)

  io.data_mem_1_ctrl_out.en   := rd_data_1_w
  io.data_mem_1_ctrl_out.we   := wr_data_1_w
  io.data_mem_1_ctrl_out.addr := Mux(wr_data_1_w, wr_addr_data_1_reg, rd_addr_data_1_reg)

  io.weight_mem_0_ctrl_out.en   := rd_weight_0_w
  io.weight_mem_0_ctrl_out.we   := wr_weight_0_w
  io.weight_mem_0_ctrl_out.addr := Mux(wr_weight_0_w, wr_addr_weight_0_reg, rd_addr_weight_0_reg)

  io.weight_mem_1_ctrl_out.en   := rd_weight_1_w
  io.weight_mem_1_ctrl_out.we   := wr_weight_1_w
  io.weight_mem_1_ctrl_out.addr := Mux(wr_weight_1_w, wr_addr_weight_1_reg, rd_addr_weight_1_reg)

  io.bias_mem_ctrl_out.en   := rd_bias_w
  io.bias_mem_ctrl_out.we   := wr_bias_w
  io.bias_mem_ctrl_out.addr := Mux(wr_bias_w, wr_addr_bias_reg, rd_addr_bias_reg)

  io.conv_start_out := conv_start_w
  io.fc_start_out   := fc_start_w

  io.data_mem_select_out   := data_mem_select_reg
  io.weight_mem_select_out := weight_mem_select_reg
  io.request_data_out      := request_data_reg
  io.conv_pool_select_out  := Mux(dim_in_counter_of_conv_reg === conv_para_reg.dim_in, true.B, false.B)

  io.done_out := done_w
}
