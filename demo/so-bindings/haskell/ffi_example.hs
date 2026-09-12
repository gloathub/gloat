{-# LANGUAGE ForeignFunctionInterface #-}

module Main where

import Foreign.C.Types
import Foreign.C.String

foreign import ccall "factorial" c_factorial :: CLLong -> CLLong
foreign import ccall "greet" c_greet :: CString -> IO CString
foreign import ccall "repeat_string" c_repeat_string :: CString -> CLLong -> IO CString
foreign import ccall "shout_it" c_shout_it :: CString -> IO ()
foreign import ccall "maybe" c_maybe :: IO CInt
foreign import ccall "sort_json_array" c_sort_json_array :: CString -> IO CString

main :: IO ()
main = do
  mapM_ (\n -> putStrLn $ show n ++ "! = " ++ show (c_factorial (fromIntegral n))) [1..10 :: Int]

  withCString "World" $ \s -> do
    r <- c_greet s
    peekCString r >>= putStrLn

  withCString "ha" $ \s -> do
    r <- c_repeat_string s 3
    peekCString r >>= putStrLn

  withCString "hello from yamlscript" $ \s ->
    c_shout_it s

  m <- c_maybe
  putStrLn $ "maybe: " ++ if m /= 0 then "true" else "false"

  withCString "[3,1,4,1,5,9,2,6]" $ \s -> do
    r <- c_sort_json_array s
    sorted <- peekCString r
    putStrLn $ "sorted: " ++ sorted
