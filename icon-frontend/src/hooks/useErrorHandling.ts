import { useState, useCallback } from "react";
import axios from "axios";
import { toast } from "react-hot-toast";
import dayjs from "dayjs";
import { log } from "console";

export interface ErrorResponse {
  status: number;
  message: string;
  timestamp: string;
}

// type ErrorHandlerOptions = {
//   onDisplayError?: (message: string) => void;
//   onAlertError?: (message: string) => void;
// };

export const useErrorHandling = () => {
  const [error, setError] = useState<ErrorResponse | null>(null);

  const handleError = useCallback((err: unknown) => {
    let errorMessage: ErrorResponse = {
      status: 400,
      message: "알 수 없는 오류가 발생했습니다.",
      timestamp: dayjs().format("YYYY-MM-DD HH:mm:ss"),
    };

    if (axios.isAxiosError(err) && err.response) {
      console.log("err.response", err.response.data);
      errorMessage = err.response.data;
    } else if (err instanceof Error) {
      console.log("error Error");
      errorMessage.message = err.message;
    }

    console.log("errorMessage: ", errorMessage);
    setError(errorMessage);
    toast.error(errorMessage.message);
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return { error, handleError, clearError };
};
