program ffi_example
  use, intrinsic :: iso_c_binding
  implicit none

  interface
    function factorial(n) bind(C, name='factorial') result(res)
      import :: c_long_long
      integer(c_long_long), value :: n
      integer(c_long_long) :: res
    end function

    function greet(name) bind(C, name='greet') result(res)
      import :: c_char, c_ptr
      character(kind=c_char), intent(in) :: name(*)
      type(c_ptr) :: res
    end function

    function repeat_string(s, n) bind(C, name='repeat_string') result(res)
      import :: c_char, c_ptr, c_long_long
      character(kind=c_char), intent(in) :: s(*)
      integer(c_long_long), value :: n
      type(c_ptr) :: res
    end function

    subroutine shout_it(s) bind(C, name='shout_it')
      import :: c_char
      character(kind=c_char), intent(in) :: s(*)
    end subroutine

    function maybe() bind(C, name='maybe') result(res)
      import :: c_int
      integer(c_int) :: res
    end function

    function sort_json_array(json) bind(C, name='sort_json_array') result(res)
      import :: c_char, c_ptr
      character(kind=c_char), intent(in) :: json(*)
      type(c_ptr) :: res
    end function
  end interface

  integer(c_long_long) :: i
  type(c_ptr) :: result_ptr

  do i = 1, 10
    write(*, '(I0, "! = ", I0)') i, factorial(i)
  end do

  result_ptr = greet("World" // c_null_char)
  call print_c_string(result_ptr)

  result_ptr = repeat_string("ha" // c_null_char, 3_c_long_long)
  call print_c_string(result_ptr)

  call shout_it("hello from yamlscript" // c_null_char)

  if (maybe() /= 0) then
    write(*, '(A)') "maybe: true"
  else
    write(*, '(A)') "maybe: false"
  end if

  result_ptr = sort_json_array("[3,1,4,1,5,9,2,6]" // c_null_char)
  write(*, '(A)', advance='no') "sorted: "
  call print_c_string(result_ptr)

contains

  subroutine print_c_string(cptr)
    type(c_ptr), intent(in) :: cptr
    character(kind=c_char), dimension(:), pointer :: chars
    integer :: length, j

    if (.not. c_associated(cptr)) return

    length = 0
    call c_f_pointer(cptr, chars, [1])
    do while (chars(length + 1) /= c_null_char)
      length = length + 1
      call c_f_pointer(cptr, chars, [length + 1])
    end do

    call c_f_pointer(cptr, chars, [length])
    do j = 1, length
      write(*, '(A)', advance='no') chars(j)
    end do
    write(*, *)
  end subroutine

end program
