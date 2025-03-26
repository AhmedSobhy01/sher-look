import { Link, redirect } from "react-router";
import { Form } from "react-router";
import Logo from "@/assets/Logo.png";
import SearchBar from "@/components/SearchBar";

const Homepage = function () {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background p-4">
      <div className="w-full max-w-md flex flex-col items-center gap-8">
        <div className="flex items-center justify-center">
          <div className="flex items-center gap-2 text-4xl font-bold">
            <img src={Logo} alt="" />
          </div>
        </div>

        <SearchBar />
      </div>
    </div>
  );
};

Homepage.action = async function ({ request }) {
  let formData = await request.formData();
  let query = formData.get("query");
  if (query) return redirect(`/search?q=${query}`);
};

export default Homepage;
