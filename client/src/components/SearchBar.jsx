import React from "react";
import { Form } from "react-router";


export default function SearchBar({ shadow = true, value = "" }) {
  return (
    <Form method="post" className="w-full space-y-4">
      <div className="flex w-full items-center space-x-2 h-11">
        <input
          name="query"
          type="search"
          placeholder="Search anything..."
		  defaultValue={value}
          className={
            "rounded-full px-4 py-2 border-zinc-300 border-1 flex-1" +
            (shadow ? " shadow-md" : "")
          }
        />
        <button
          type="submit"
          className="bg-zinc-800 text-zinc-100 px-3 py-2 rounded-full cursor-pointer"
        >
          Search
        </button>
      </div>
    </Form>
  );
}
