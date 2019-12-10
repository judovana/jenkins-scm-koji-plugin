import React from "react"
import { stores } from "../stores"

export default () => React.useContext(React.createContext<typeof stores>(stores))
