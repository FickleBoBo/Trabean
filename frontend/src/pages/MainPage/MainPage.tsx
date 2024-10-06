import { useEffect, useState } from "react";

import Account from "./MainPage.Account";

import logo from "../../assets/logo.png";
import plusIcon from "../../assets/icon/plusIcon.png";
import client from "../../client";

type AccountType = {
  accountId: number;
  accountNo: string;
  accountName: string;
  bankName: string;
  accountBalance: number;
};

function MainPage() {
  const [mainAccount, setMainAccount] = useState<AccountType | null>(null);
  const [accountList, setAccountList] = useState<Array<AccountType> | null>(
    null,
  );

  useEffect(() => {
    const getAccountInfo = async () => {
      const response = await client().get("/api/accounts");
      setMainAccount(response.data.mainAccount);
      setAccountList(response.data.accountList);
    };

    getAccountInfo();
    // eslint-disable-next-line
  }, []);

  return (
    <div className="px-6 py-20">
      <img src={logo} alt="로고" className="h-5 mb-3" />
      <div className="bg-primary-light h-56 rounded-2xl py-5 px-6 flex flex-col justify-between">
        <div>
          <h4 className="text-lg font-bold">{mainAccount?.accountName}</h4>
          <span className="text-sm">{mainAccount?.accountNo}</span>
        </div>
        <div className="text-3xl font-bold">{mainAccount?.accountBalance}</div>
        <button type="button" className="btn-lg">
          이체하기
        </button>
      </div>
      <div className="border-[1.5px] border-primary border-solid rounded-xl px-1 flex flex-col justify-between mt-5">
        {accountList?.map((account, index) => (
          <div key={account.accountId}>
            {index > 0 && (
              <div className="border-[1px] border-solid border-primary w-full" />
            )}
            <Account account={account} />
          </div>
        ))}
      </div>
      <div className="mt-5 border-[1.5px] border-primary-light border-solid rounded-lg flex justify-center items-center h-11">
        <img src={plusIcon} alt="통장추가버튼" className="h-5" />
      </div>
    </div>
  );
}

export default MainPage;