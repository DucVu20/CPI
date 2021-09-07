import chisel3._
import scala.math._

class single_port_ram[T <: Data](MEM_SIZE  : Int,
                                 DATA_TYPE : T) extends Module {
  val MEM_WIDTH : Int = ceil(log(MEM_SIZE)/log(2)).toInt

  val io = IO(new Bundle {
    val control  : mem_control_io = Input(new mem_control_io(MEM_WIDTH))
    val data_in  : T = Input(DATA_TYPE)
    val data_out : T = Output(DATA_TYPE)
  })

  val mem = SyncReadMem(MEM_SIZE, DATA_TYPE)

  when (io.control.we) {
    mem.write(io.control.addr, io.data_in.asTypeOf(DATA_TYPE))
  }
  io.data_out := mem.read(io.control.addr, io.control.en)
}
