use libloading::{Library, Symbol};
use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_longlong};

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Load the shared library from LD_LIBRARY_PATH
    let lib = unsafe { Library::new("example.so")? };

    // factorial(int) -> int
    let factorial: Symbol<unsafe extern "C" fn(c_longlong) -> c_longlong> =
        unsafe { lib.get(b"factorial")? };

    for n in 1..=10 {
        let result = unsafe { factorial(n) };
        println!("{}! = {}", n, result);
    }

    // greet(str) -> str
    let greet: Symbol<unsafe extern "C" fn(*const c_char) -> *mut c_char> =
        unsafe { lib.get(b"greet")? };

    let name = CString::new("World")?;
    let result = unsafe { greet(name.as_ptr()) };
    let result_str = unsafe { CStr::from_ptr(result).to_str()? };
    println!("{}", result_str);

    // repeat_string(str, int) -> str
    let repeat_string: Symbol<unsafe extern "C" fn(*const c_char, c_longlong) -> *mut c_char> =
        unsafe { lib.get(b"repeat_string")? };

    let s = CString::new("ha")?;
    let result = unsafe { repeat_string(s.as_ptr(), 3) };
    let result_str = unsafe { CStr::from_ptr(result).to_str()? };
    println!("{}", result_str);

    // shout_it(str) -> void
    let shout_it: Symbol<unsafe extern "C" fn(*const c_char)> =
        unsafe { lib.get(b"shout_it")? };

    let msg = CString::new("hello from yamlscript")?;
    unsafe { shout_it(msg.as_ptr()) };

    // maybe() -> bool
    let maybe: Symbol<unsafe extern "C" fn() -> c_int> =
        unsafe { lib.get(b"maybe")? };

    let result = unsafe { maybe() };
    println!("maybe: {}", result != 0);

    // sort_json_array(str) -> str
    let sort_json_array: Symbol<unsafe extern "C" fn(*const c_char) -> *mut c_char> =
        unsafe { lib.get(b"sort_json_array")? };

    let data = serde_json::json!([3, 1, 4, 1, 5, 9, 2, 6]).to_string();
    let data_cstr = CString::new(data)?;
    let result = unsafe { sort_json_array(data_cstr.as_ptr()) };
    let result_str = unsafe { CStr::from_ptr(result).to_str()? };
    println!("sorted: {}", result_str);

    Ok(())
}
