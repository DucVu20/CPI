import chisel3._
import scala.math._

class memory_control_unit[T <: Data](MEM_SIZE : Int,
                                     NUM_CONCURRENT : Int,
                                     DATA_TYPE : T) extends Module {
  val MEM_WIDTH : Int = ceil(log(MEM_SIZE)/log(2)).toInt
  val MAC_WIDTH : Int = ceil(log(NUM_CONCURRENT)/log(2)).toInt

  def gen_counter(max_value : UInt, ena : Bool) : UInt = {
    val counter_reg = RegInit(0.U(max_value.getWidth.W))
    counter_reg := Mux(counter_reg === max_value, 0.U, Mux(ena, counter_reg + 1.U, counter_reg))
    counter_reg
  }

  val io = IO(new Bundle {
    val control  : mem_control_io = Input(mem_control_io(MEM_WIDTH))
    val data_in  : T = Input(DATA_TYPE)
    val data_out : Vec[T] = Output(Vec(NUM_CONCURRENT, DATA_TYPE))
  })

  val group_mem : Array[single_port_ram[T]] = Array.fill(NUM_CONCURRENT) {
    Module(new single_port_ram[T](MEM_SIZE = ceil(MEM_SIZE/NUM_CONCURRENT).toInt, DATA_TYPE = DATA_TYPE))
  }

  val mac_counter_reg : UInt = gen_counter((NUM_CONCURRENT - 1).asUInt(), io.control.we)
  val tmp_we_w : Vec[UInt] = WireInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.U(1.W))))

  when (io.control.we) {
    tmp_we_w := WireInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.U(1.W))))
    tmp_we_w(mac_counter_reg) := 1.U(1.W)
  }.otherwise(
    tmp_we_w := WireInit(VecInit(Seq.fill(NUM_CONCURRENT)(0.U(1.W))))
  )

  for (i <- 0 until NUM_CONCURRENT) {
    group_mem(i).io.control.we <> tmp_we_w(i)
    group_mem(i).io.control.en <> io.control.en
    group_mem(i).io.control.addr <> Mux(io.control.we, (io.control.addr >> MAC_WIDTH).asUInt(), io.control.addr)
    group_mem(i).io.data_in <> io.data_in
    when (mac_counter_reg === i.asUInt()) {
      group_mem(i).io.control.we <> io.control.we
    }.otherwise {
      group_mem(i).io.control.we <> false.B
    }
    io.data_out(i) <> group_mem(i).io.data_out
  }
}
