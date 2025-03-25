import React from "react";
import SearchBar from "@/components/SearchBar";
import { useLoaderData } from "react-router";
import Logo from "@/assets/Logo.png";
import { useNavigate } from "react-router";

const ResultsPage = function () {
  const navigate = useNavigate();
  const data = useLoaderData();
  console.log(data);
  return (
    <div>
      <header className="flex items-center justify-left mb-4 shadow-sm py-4 px-8">
        <img
          src={Logo}
          alt=""
          className="h-11 w-26 object-cover cursor-pointer ml-8 hidden sm:block mr-2 md:mr-4 lg:mr-8 md:ml-12 lg:ml-0"
          onClick={() => navigate("/")}
        />
        <div className="max-w-2xl w-full">
          <SearchBar shadow={false} value={data.query} />
        </div>
      </header>
      <main className="max-w-3xl mr-8 ml-8 sm:ml-16 md:ml-20 lg:ml-42">
        <p className="text-zinc-600 text-sm">
          About {data.data.length} results for {data.query}
        </p>
        <ul>
          {data.data.map((result) => (
            <li key={result.Url} className="gap-4 my-4">
              <a href={result.Url} target="_blank" rel="noopener noreferrer">
                <h2 className="text-xl font-semibold hover:underline">
                  {result.title}
                </h2>
                <p className="text-slate-800 text-sm">{result.Url}</p>
                <p className="text-zinc-500 text-sm">{result.description}</p>
              </a>
            </li>
          ))}
        </ul>
      </main>
    </div>
  );
};

ResultsPage.loader = async function ({ request }) {
  const searchParams = new URL(request.url).searchParams;
  const searchTerm = searchParams.get("q");


  // TODO: Fetch search results from the backend
  const dummyResponse = {
    query: searchTerm,
    data: [
      {
        Url: "https://www.google.com",
        title: "Google",
        description:
          "Search the world's information, including webpages, images, videos and more. Google has many special features to help you find exactly what you're looking for.",
      },
      {
        Url: "https://www.bing.com",
        title: "Bing",
        description:
          "Bing helps you turn information into action, making it faster and easier to go from searching to doing.",
      },
      {
        Url: "https://www.yahoo.com",
        title: "Yahoo",
        description:
          "News, email and search are just the beginning. Discover more every day. Find your yodel.",
      },
    ],
  };
  return dummyResponse;
};

export default ResultsPage;
