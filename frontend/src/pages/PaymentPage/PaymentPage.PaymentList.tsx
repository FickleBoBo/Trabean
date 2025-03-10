import React, { useState } from "react";

const PaymentList: React.FC = () => {
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [showDate, setShowDate] = useState(false);
  const [isTextInput, setIsTextInput] = useState(true);
  // const [dateAlert, setDateAlert] = useState("");

  // 날짜 선택
  const handleDateChange = (
    e: React.ChangeEvent<HTMLInputElement>,
    date: string,
  ) => {
    const newValue = e.target.value;

    if (date === "start") {
      // 시작일이 종료일보다 클 경우 시작일을 종료일로 설정
      if (endDate && new Date(newValue) > new Date(endDate)) {
        if (new Date(newValue) > new Date(endDate)) {
          setStartDate(endDate);
        }
        return;
      }
      const today = new Date();
      today.setHours(0, 0, 0, 0); // 시간 부분을 00:00:00으로 설정
      if (new Date(newValue) > today) {
        // 시작일이 오늘 날짜보다 미래일 경우, 시작일을 오늘 날짜로 설정
        const year = today.getFullYear().toString();
        const month = (today.getMonth() + 1).toString().padStart(2, "0"); // 월은 0부터 시작하므로 +1, 2자리로 맞춤
        const day = today.getDate().toString().padStart(2, "0"); // 날짜를 2자리로 맞춤
        const formattedDate = `${year}-${month}-${day}`; // 'YYYY-MM-DD' 형식으로 변환

        setEndDate(formattedDate);
        return;
      }
      setStartDate(newValue);
    } else if (date === "end") {
      const today = new Date();
      today.setHours(0, 0, 0, 0); // 시간 부분을 00:00:00으로 설정
      if (new Date(newValue) > today) {
        // 종료일이 오늘 날짜보다 미래일 경우, 종료일을 오늘 날짜로 설정
        const year = today.getFullYear().toString();
        const month = (today.getMonth() + 1).toString().padStart(2, "0"); // 월은 0부터 시작하므로 +1, 2자리로 맞춤
        const day = today.getDate().toString().padStart(2, "0"); // 날짜를 2자리로 맞춤
        const formattedDate = `${year}-${month}-${day}`; // 'YYYY-MM-DD' 형식으로 변환

        setEndDate(formattedDate);
        return;
      }
      setEndDate(newValue);
    }
  };

  const toggleDate = () => {
    if (showDate) {
      setStartDate("");
      setEndDate("");
      setShowDate(false);
      setIsTextInput(true);
    } else if (!showDate) {
      setShowDate(true);
    }
  };

  return (
    <div className="w-full bg-[#F4F4F5] pt-[4.375rem] flex flex-col items-center">
      <div className="w-[300px]">
        <h1 className="text-base text-gray-900 font-semibold flex justify-between">
          지출 내역
          <button
            type="button"
            className="font-semibold focus:cursor-pointer"
            onClick={toggleDate}
          >
            {showDate ? "선택 취소" : "기간 선택"}{" "}
            <span className="text-xs mx-1">▼</span>
          </button>
        </h1>
        {showDate && (
          <div
            id="date-select"
            className="w-full bg-white rounded-[0.625rem] px-[1rem] mt-[1.25rem] flex flex-col justify-center items-center"
          >
            <div className="w-full flex mt-3">
              <input
                type="text"
                placeholder="조회 시작일"
                value={startDate}
                onFocus={(e) => {
                  e.target.type = "date";
                  setIsTextInput(false);
                }}
                onBlur={(e) => {
                  if (e.target.value === "") {
                    e.target.type = "text";
                  }
                }}
                onChange={(e) => handleDateChange(e, "start")}
                className="bg-transparent p-2 w-1/2 text-center focus:outline-none text-sm"
              />
              <span className="mx-1">~</span>
              <input
                type="text"
                placeholder="조회 종료일"
                value={endDate}
                onFocus={(e) => {
                  e.target.type = "date";
                  setIsTextInput(false);
                }}
                onBlur={(e) => {
                  if (e.target.value === "") {
                    e.target.type = "text";
                  }
                }}
                onChange={(e) => handleDateChange(e, "end")}
                className="bg-transparent p-2 w-1/2 text-center focus:outline-none text-sm"
              />
            </div>
            <button
              type="button"
              className="mt-2 btn-light-md w-[90%] focus:cursor-pointer hover:btn-md"
            >
              조회
            </button>
            {isTextInput && (
              <p className="text-xs text-green-800 w-full my-2 text-center mb-3 mt-3">
                지정하지 않으면 모든 결제 기록을 확인할 수 있어요!
              </p>
            )}
          </div>
        )}

        <div className="w-full bg-white rounded-[0.625rem] px-[1rem] my-[1.25rem]">
          <p className="font-semibold text-gray-700 text-sm h-[2.5rem] flex items-center">
            <span className="w-[60%]">총 지출액</span>{" "}
            <span className="w-[60%]">42,000</span>
          </p>
        </div>

        <div
          id="chart"
          className="bg-white rounded-[15px] py-[0.875rem] px-[1rem]"
        >
          <p>차트영역</p>
        </div>
      </div>
    </div>
  );
};

export default PaymentList;
