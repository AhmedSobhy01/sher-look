import { Link, redirect } from "react-router";
import { Form } from "react-router";
import Logo from "@/assets/Logo.png";
import SearchBar from "@/components/SearchBar";
import { useEffect, useState } from "react";

const Homepage = function () {
    const [isLoaded, setIsLoaded] = useState(false);

    useEffect(() => setIsLoaded(true), []);

    return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-b from-white to-blue-50 p-4">
            <div className={`w-full max-w-md flex flex-col items-center gap-12 transition-all duration-700 ${isLoaded ? "opacity-100" : "opacity-0 translate-y-8"}`}>
                <div className={`flex items-center justify-center transition-transform duration-500 delay-300 ${isLoaded ? "scale-100" : "scale-90"}`}>
                    <div className="flex items-center gap-2 text-4xl font-bold">
                        <img src={Logo} className={`h-24 w-auto ${isLoaded ? "animate-pop" : ""} hover:animate-bounce-once`} />
                    </div>
                </div>

                <div className={`w-full transition-all duration-500 delay-500 ${isLoaded ? "opacity-100 translate-y-0" : "opacity-0 translate-y-8"}`}>
                    <SearchBar />
                </div>
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
